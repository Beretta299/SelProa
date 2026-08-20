package selproa.registry

import java.sql.Statement
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.sql.DataSource
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Generates the registry.
 *
 * Deterministic on [SEED], so the ground truth is regenerable rather than guarded.
 * Run: ./gradlew seed   (REGISTRY_SEED_COUNT vehicles, default 20000)
 */
const val SEED = 20260820L

data class Planted(val vin: String, val pattern: String, val evidence: String)

private data class Spec(val make: String, val model: String, val wmi: String,
                        val fuels: List<String>, val engines: List<String>, val weight: Int)

private val SPECS = listOf(
    Spec("Volkswagen", "Golf", "WVW", listOf("diesel", "petrol"), listOf("CLHA", "CZCA", "DADA"), 10),
    Spec("Volkswagen", "Passat", "WVW", listOf("diesel"), listOf("CRLB", "DFGA"), 7),
    Spec("Skoda", "Octavia", "TMB", listOf("diesel", "petrol"), listOf("CLHA", "DFGA"), 9),
    Spec("Audi", "A4", "WAU", listOf("diesel", "petrol"), listOf("CNHA", "CJEB"), 7),
    Spec("BMW", "3 Series", "WBA", listOf("diesel", "petrol"), listOf("N47D20", "B47D20", "N20B20"), 7),
    Spec("BMW", "5 Series", "WBA", listOf("diesel"), listOf("N57D30", "B47D20"), 4),
    Spec("Mercedes-Benz", "C-Class", "WDD", listOf("diesel", "petrol"), listOf("OM651", "M274"), 5),
    Spec("Opel", "Astra", "W0L", listOf("diesel", "petrol"), listOf("B16DTH", "B14XFT"), 8),
    Spec("Ford", "Focus", "WF0", listOf("diesel", "petrol"), listOf("XWDA", "M1DA"), 7),
    Spec("Toyota", "Corolla", "SB1", listOf("petrol", "hybrid"), listOf("1ZR-FAE", "2ZR-FXE"), 6),
    Spec("Renault", "Megane", "VF1", listOf("diesel"), listOf("K9K", "R9M"), 5),
    Spec("Peugeot", "308", "VF3", listOf("diesel", "petrol"), listOf("DV6", "EB2"), 5),
    Spec("Nissan", "Qashqai", "SJN", listOf("diesel", "petrol"), listOf("K9K", "R9M"), 6),
    Spec("Kia", "Ceed", "KNA", listOf("diesel", "petrol"), listOf("D4FB", "G3LC"), 5),
    Spec("Hyundai", "i30", "TMA", listOf("diesel", "petrol"), listOf("D4FB", "G4LD"), 5),
)

private val CITIES = listOf(
    "Warszawa" to "mazowieckie", "Kraków" to "małopolskie", "Wrocław" to "dolnośląskie",
    "Poznań" to "wielkopolskie", "Gdańsk" to "pomorskie", "Łódź" to "łódzkie",
    "Katowice" to "śląskie", "Szczecin" to "zachodniopomorskie", "Lublin" to "lubelskie",
    "Białystok" to "podlaskie", "Rzeszów" to "podkarpackie", "Bydgoszcz" to "kujawsko-pomorskie",
)

private data class Event(
    val type: String, val on: LocalDate, val odo: Int?,
    val country: String, val source: String, val detail: String = "{}",
)

private class Car(
    val vin: String,
    val spec: Spec,
    val year: Int,
    val fuel: String,
    val engine: String,
    val power: Int,
    val checkDigitValid: Boolean,
    val events: MutableList<Event> = mutableListOf(),
)

class Seeder(private val ds: DataSource, private val rng: Random = Random(SEED)) {

    private val planted = mutableListOf<Planted>()
    private val today: LocalDate = LocalDate.of(2026, 8, 20)
    private val weighted = SPECS.flatMap { s -> List(s.weight) { s } }

    private fun pickImported(cars: List<Car>, used: MutableSet<Car>, n: Int) =
        cars.filter { it !in used && it.events.any { e -> e.type == "import" } }
            .shuffled(rng).take(n).also { used += it }

    fun run(count: Int): List<Planted> {
        val cars = (1..count).map { car() }
        val used = mutableSetOf<Car>()
        fun pick(n: Int) = cars.filter { it !in used }.shuffled(rng).take(n).also { used += it }

        rollbacks(pick(25))
        clonedVins(pick(12))
        washedWriteOffs(pickImported(cars, used, 15))
        forgedVins(pick(10))
        impossibleRates(pick(12))

        write(cars)
        sellers(cars)
        return planted
    }

    // ── generation ────────────────────────────────────────────────────────

    private fun car(): Car {
        val spec = weighted.random(rng)
        val year = (2008..2022).random(rng)
        val c = Car(
            vin = Vin.generate(spec.wmi, rng),
            spec = spec,
            year = year,
            fuel = spec.fuels.random(rng),
            engine = spec.engines.random(rng),
            power = listOf(90, 105, 110, 120, 136, 150, 163, 184, 190, 218).random(rng),
            checkDigitValid = true,
        )
        timeline(c)
        return c
    }

    /**
     * A believable Polish ownership timeline.
     *
     * Built as dates first, then walked in order, so that two invariants hold by
     * construction: the odometer never decreases, and no Polish record predates
     * the import. Both matter -- without them every imported car looks like a
     * cloned VIN, and the planted fraud would be indistinguishable from noise.
     */
    private fun timeline(c: Car) {
        val firstReg = LocalDate.of(c.year, (1..12).random(rng), (1..28).random(rng))
        val kmPerYear = rng.nextInt(8_000, 26_000)

        val imported = rng.nextInt(100) < 35
        val origin = if (imported) listOf("DE", "FR", "NL", "BE", "IT").random(rng) else "PL"
        val importOn = if (imported)
            firstReg.plusYears(rng.nextLong(2, 8)).coerceAtMost(today.minusMonths(6)) else null

        // 1. Collect the dates and what happened, in no particular order.
        val marks = mutableListOf<Pair<LocalDate, String>>()
        marks += firstReg to "registration"
        if (importOn != null) {
            marks += importOn to "import"
            marks += importOn.plusDays(rng.nextLong(3, 40)) to "registration"
        }

        var next = firstReg.plusYears(3)
        while (next.isBefore(today)) {
            marks += next to "technical_inspection"
            next = next.plusYears(if (next.isBefore(firstReg.plusYears(5))) 2 else 1)
        }
        repeat(rng.nextInt(0, 4)) { marks += randomDateBetween(firstReg, today) to "ownership_change" }
        repeat(rng.nextInt(1, 6)) { marks += randomDateBetween(firstReg, today) to "service" }
        if (rng.nextInt(100) < 18) marks += randomDateBetween(firstReg.plusYears(1), today) to "damage_claim"

        // 2. Walk them in order, carrying one monotonic odometer and the country
        //    the car was actually in at the time.
        marks.sortBy { it.first }
        marks.forEach { (on, type) ->
            val country = if (importOn != null && on.isBefore(importOn)) origin else "PL"
            val elapsed = ChronoUnit.DAYS.between(firstReg, on).coerceAtLeast(0)
            val odo = (kmPerYear * elapsed / 365.0).roundToInt()

            val source = when {
                country != "PL" -> if (type == "service") "service_network" else "foreign_registry"
                type == "service" -> "service_network"
                type == "damage_claim" -> "insurer"
                type == "import" -> "customs"
                else -> "cepik"
            }
            // Only the record types that actually read an odometer carry one.
            val reading = if (type in setOf("technical_inspection", "service", "import")) odo else null
            val detail = when (type) {
                "import" -> """{"from":"$origin"}"""
                "service" -> """{"work":"${listOf("oil change", "brakes", "timing chain", "clutch", "suspension").random(rng)}"}"""
                "damage_claim" -> """{"severity":"${listOf("minor", "moderate").random(rng)}","area":"${listOf("front", "rear", "side").random(rng)}"}"""
                else -> "{}"
            }
            c.events += Event(type, on, reading, country, source, detail)
        }
    }

    private fun odoAt(c: Car, on: LocalDate): Int =
        c.events.filter { it.odo != null && !it.on.isAfter(on) }.maxOfOrNull { it.odo!! }
            ?: c.events.firstOrNull { it.odo != null }?.odo ?: 0

    private fun randomDateBetween(a: LocalDate, b: LocalDate): LocalDate {
        val days = ChronoUnit.DAYS.between(a, b).coerceAtLeast(1)
        return a.plusDays(rng.nextLong(0, days))
    }

    // ── fraud ─────────────────────────────────────────────────────────────

    /** A later reading is lower than an earlier one. The classic. */
    private fun rollbacks(cars: List<Car>) = cars.forEach { c ->
        val readings = c.events.withIndex().filter { it.value.odo != null && it.value.odo!! > 30_000 }
        if (readings.size < 3) return@forEach
        val cut = readings[readings.size - 2]
        val before = readings[readings.size - 3].value.odo!!
        val rolled = (before * rng.nextDouble(0.45, 0.7)).roundToInt()
        c.events[cut.index] = cut.value.copy(odo = rolled)
        // Everything after it continues from the lowered figure.
        readings.filter { it.index > cut.index }.forEach { (i, e) ->
            c.events[i] = e.copy(odo = rolled + rng.nextInt(3_000, 20_000))
        }
        planted += Planted(c.vin, "odometer_rollback",
            "inspection on ${cut.value.on} reads ${rolled}km after an earlier reading of ${before}km")
    }

    /** The same VIN with events in two countries at the same time. Physically impossible. */
    private fun clonedVins(cars: List<Car>) = cars.forEach { c ->
        val country = listOf("DE", "LT", "IT", "ES").random(rng)
        val overlap = randomDateBetween(today.minusYears(3), today.minusMonths(2))
        c.events += Event("technical_inspection", overlap, rng.nextInt(80_000, 260_000), country, "foreign_registry")
        c.events += Event("ownership_change", overlap.plusDays(rng.nextLong(5, 60)), null, country, "foreign_registry")
        c.events.sortBy { it.on }
        planted += Planted(c.vin, "cloned_vin",
            "events recorded in $country around $overlap while the car was registered and inspected in PL")
    }

    /**
     * Written off abroad, imported, re-registered as clean.
     *
     * Only applied to cars that were actually imported. Bolting an import onto a
     * car with ten years of Polish inspections produces a timeline no real
     * registry could contain, and the detector would learn to spot my generator
     * rather than the fraud.
     */
    private fun washedWriteOffs(cars: List<Car>) = cars.forEach { c ->
        val importEvent = c.events.firstOrNull { it.type == "import" } ?: return@forEach
        val from = Regex("\"from\":\"(\\w+)\"").find(importEvent.detail)?.groupValues?.get(1) ?: "DE"
        val on = importEvent.on.minusMonths(rng.nextLong(1, 10))
        c.events += Event("total_loss", on, null, from, "foreign_insurer",
            """{"cause":"${listOf("collision", "flood", "fire").random(rng)}","settlement":"paid"}""")
        c.events.sortBy { it.on }
        planted += Planted(c.vin, "washed_write_off",
            "declared a total loss in $from on $on, then imported on ${importEvent.on} " +
                "and re-registered in PL with no damage record here")
    }

    /** A VIN whose ninth character does not check out. */
    private fun forgedVins(cars: List<Car>) = cars.forEach { c ->
        val bad = Vin.generateInvalid(c.spec.wmi, rng)
        forged[c] = bad
        planted += Planted(bad, "invalid_check_digit",
            "ninth character is '${bad[8]}', the checksum requires '${Vin.checkDigit(bad)}'")
    }
    private val forged = mutableMapOf<Car, String>()

    /** Readings that are individually plausible and impossible together. */
    private fun impossibleRates(cars: List<Car>) = cars.forEach { c ->
        val idx = c.events.withIndex().filter { it.value.odo != null }
        if (idx.size < 2) return@forEach
        val (i, e) = idx.last()
        val (_, prev) = idx[idx.size - 2]
        val days = ChronoUnit.DAYS.between(prev.on, e.on).coerceAtLeast(1)
        val jumped = prev.odo!! + (days * rng.nextInt(600, 1200)).toInt()
        c.events[i] = e.copy(odo = jumped)
        planted += Planted(c.vin, "impossible_mileage_rate",
            "${jumped - prev.odo!!}km between ${prev.on} and ${e.on}, about ${(jumped - prev.odo!!) / days}km per day")
    }

    // ── sellers ───────────────────────────────────────────────────────────

    /**
     * Who advertised what.
     *
     * Most private sellers appear once or twice. Dealers appear often. The
     * interesting cases are the ones in between: a number claiming to be private
     * with thirty cars, and a number whose cars are disproportionately dirty.
     * Neither is visible from a single VIN, which is the point.
     */
    private fun sellers(cars: List<Car>) = ds.use { c ->
        val vinOf = { car: Car -> forged[car] ?: car.vin }
        val dirty = planted.map { it.vin }.toSet()
        val dirtyCars = cars.filter { vinOf(it) in dirty }.toMutableList()
        val cleanCars = cars.filter { vinOf(it) !in dirty }.shuffled(rng).toMutableList()

        data class Contact(val phone: String, val kind: String, val vins: List<String>, val cities: List<Int>)
        val contacts = mutableListOf<Contact>()

        fun phone() = "+48${rng.nextInt(500, 899)}${"%03d".format(rng.nextInt(0, 1000))}${"%03d".format(rng.nextInt(0, 1000))}"
        fun takeClean(n: Int) = (0 until minOf(n, cleanCars.size)).map { cleanCars.removeFirst() }.map(vinOf)

        // Three rings: a number claiming to be private whose cars are mostly frauds.
        repeat(3) {
            val ring = (0 until minOf(rng.nextInt(6, 10), dirtyCars.size)).map { dirtyCars.removeFirst() }.map(vinOf)
            if (ring.isEmpty()) return@repeat
            val padding = takeClean(rng.nextInt(1, 4))
            val p = phone()
            contacts += Contact(p, "private", ring + padding, listOf(rng.nextInt(0, CITIES.size)))
            planted += Planted(p, "seller_fraud_ring",
                "number ending ${p.takeLast(3)} advertised ${ring.size + padding.size} cars, " +
                    "${ring.size} of which carry a history problem")
        }

        // Unregistered dealers: claimed private, far too many cars, spread over cities.
        repeat(6) {
            val vins = takeClean(rng.nextInt(14, 34))
            if (vins.isEmpty()) return@repeat
            val p = phone()
            val cityIdx = (1..rng.nextInt(3, 7)).map { rng.nextInt(0, CITIES.size) }.distinct()
            contacts += Contact(p, "private", vins, cityIdx)
            planted += Planted(p, "unregistered_dealer",
                "number ending ${p.takeLast(3)} advertised ${vins.size} cars across " +
                    "${cityIdx.size} voivodeships while claiming to be a private seller")
        }

        // Ordinary traffic: real dealers, and private sellers with one or two cars.
        repeat(40) {
            val vins = takeClean(rng.nextInt(8, 30))
            if (vins.isNotEmpty())
                contacts += Contact(phone(), "dealer", vins, (1..rng.nextInt(1, 3)).map { rng.nextInt(0, CITIES.size) })
        }
        while (cleanCars.isNotEmpty() || dirtyCars.isNotEmpty()) {
            val vins = (takeClean(rng.nextInt(1, 3)) +
                (if (dirtyCars.isNotEmpty() && rng.nextInt(100) < 40) listOf(vinOf(dirtyCars.removeFirst())) else emptyList()))
            if (vins.isEmpty()) break
            contacts += Contact(phone(), "private", vins, listOf(rng.nextInt(0, CITIES.size)))
        }

        val ids = mutableMapOf<String, Long>()
        c.prepareStatement(
            """insert into seller_contacts (phone_sha256, phone_suffix, claimed_kind, first_seen_on, last_seen_on)
               values (?,?,?,?,?) returning id"""
        ).use { st ->
            contacts.forEach { ct ->
                st.setString(1, Phone.hash(ct.phone))
                st.setString(2, Phone.suffix(ct.phone))
                st.setString(3, ct.kind)
                st.setDate(4, java.sql.Date.valueOf(today.minusDays(rng.nextLong(120, 900))))
                st.setDate(5, java.sql.Date.valueOf(today.minusDays(rng.nextLong(0, 60))))
                st.executeQuery().map { it.getLong("id") }.first().let { ids[ct.phone] = it }
            }
        }

        c.prepareStatement(
            """insert into advert_sightings (vin, contact_id, seen_on, city, voivodeship, source)
               values (?,?,?,?,?,'aggregator') on conflict do nothing"""
        ).use { st ->
            contacts.forEach { ct ->
                val id = ids[ct.phone]!!
                ct.vins.forEach { vin ->
                    val (city, voi) = CITIES[ct.cities.random(rng)]
                    st.setString(1, vin); st.setLong(2, id)
                    st.setDate(3, java.sql.Date.valueOf(today.minusDays(rng.nextLong(0, 400))))
                    st.setString(4, city); st.setString(5, voi)
                    st.addBatch()
                }
            }
            st.executeBatch()
        }
    }

    // ── writing ───────────────────────────────────────────────────────────

    private fun write(cars: List<Car>) = ds.use { c ->
        c.autoCommit = false
        c.createStatement().use {
            it.execute("truncate inspection_referrals, advert_sightings, seller_contacts, history_events, vehicles, garages restart identity cascade")
        }

        c.prepareStatement(
            """insert into vehicles (vin, make, model, model_year, body_class, fuel_type,
               displacement_l, engine_code, power_hp, plant_country, check_digit_valid)
               values (?,?,?,?,?,?,?,?,?,?,?)"""
        ).use { st ->
            cars.forEach { car ->
                val vin = forged[car] ?: car.vin
                st.setString(1, vin)
                st.setString(2, car.spec.make); st.setString(3, car.spec.model)
                st.setInt(4, car.year)
                st.setString(5, listOf("Hatchback", "Sedan/Saloon", "Wagon", "SUV").random(rng))
                st.setString(6, car.fuel)
                st.setBigDecimal(7, java.math.BigDecimal.valueOf(listOf(1.4, 1.6, 2.0, 3.0).random(rng)))
                st.setString(8, car.engine); st.setInt(9, car.power)
                st.setString(10, listOf("GERMANY", "SPAIN", "CZECHIA", "POLAND", "FRANCE").random(rng))
                st.setBoolean(11, Vin.isValid(vin))
                st.addBatch()
            }
            st.executeBatch()
        }

        c.prepareStatement(
            "insert into history_events (vin, event_type, occurred_on, odometer_km, country, source, detail) " +
                "values (?,?,?,?,?,?,?::jsonb)"
        ).use { st ->
            cars.forEach { car ->
                val vin = forged[car] ?: car.vin
                car.events.forEach { e ->
                    st.setString(1, vin); st.setString(2, e.type)
                    st.setDate(3, java.sql.Date.valueOf(e.on))
                    if (e.odo == null) st.setNull(4, java.sql.Types.INTEGER) else st.setInt(4, e.odo)
                    st.setString(5, e.country); st.setString(6, e.source); st.setString(7, e.detail)
                    st.addBatch()
                }
            }
            st.executeBatch()
        }

        insertGarages(c)
        c.commit()
    }

    private fun insertGarages(c: java.sql.Connection) {
        val names = listOf("Auto Serwis", "Warsztat", "Mechanika Pojazdowa", "Serwis Samochodowy", "AutoTechnika")
        val surnames = listOf("Kowalski", "Nowak", "Wiśniewski", "Wójcik", "Kamiński", "Lewandowski", "Zieliński")
        val specialties = listOf("diagnostyka", "blacharstwo", "lakiernictwo", "elektryka", "diesel",
            "skrzynie automatyczne", "geometria", "klimatyzacja", "inspekcja przedzakupowa")

        c.prepareStatement(
            """insert into garages (name, city, voivodeship, address, specialties, partner, rating,
               inspection_price_pln, phone, accepts_until) values (?,?,?,?,?,?,?,?,?,?)"""
        ).use { st ->
            repeat(240) { i ->
                val (city, voi) = CITIES.random(rng)
                val partner = rng.nextInt(100) < 55
                st.setString(1, "${names.random(rng)} ${surnames.random(rng)}")
                st.setString(2, city); st.setString(3, voi)
                st.setString(4, "ul. ${surnames.random(rng)}a ${rng.nextInt(1, 120)}")
                st.setArray(5, c.createArrayOf("text", specialties.shuffled(rng).take(rng.nextInt(1, 4)).toTypedArray()))
                st.setBoolean(6, partner)
                st.setBigDecimal(7, java.math.BigDecimal.valueOf(rng.nextInt(35, 50) / 10.0))
                st.setInt(8, listOf(150, 200, 250, 300, 350, 400).random(rng))
                st.setString(9, "+48 ${rng.nextInt(500, 899)} ${rng.nextInt(100, 999)} ${rng.nextInt(100, 999)}")
                // A tenth of partners have lapsed. Booking one of those triggers fault 3.
                if (partner && i % 10 == 0)
                    st.setDate(10, java.sql.Date.valueOf(today.minusMonths(2)))
                else st.setNull(10, java.sql.Types.DATE)
                st.addBatch()
            }
            st.executeBatch()
        }
    }
}

fun main() {
    val config = Config.fromEnv()
    val ds = dataSource(config)
    migrate(ds)

    val count = (System.getenv("REGISTRY_SEED_COUNT") ?: "20000").toInt()
    println("seeding $count vehicles (rng seed $SEED)…")
    val t0 = System.currentTimeMillis()
    val planted = Seeder(ds).run(count)

    java.io.File("seed-truth.json").writeText(buildString {
        append("{\n  \"rng_seed\": $SEED,\n  \"vehicle_count\": $count,\n  \"planted\": [\n")
        append(planted.joinToString(",\n") {
            """    {"vin": "${it.vin}", "pattern": "${it.pattern}", "evidence": ${esc(it.evidence)}}"""
        })
        append("\n  ]\n}\n")
    })

    println("done in ${"%.1f".format((System.currentTimeMillis() - t0) / 1000.0)}s — ${planted.size} planted")
    planted.groupingBy { it.pattern }.eachCount().toSortedMap().forEach { (k, v) -> println("  $k: $v") }
    println("\nDo not open seed-truth.json again until chapter 25.")
}

private fun esc(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
