package pitwall.market

import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.sql.DataSource
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Generates the market.
 *
 * Deterministic: the same [SEED] always produces the same listings and the same
 * planted fraud, so the ground-truth file can be regenerated rather than guarded.
 *
 * Run:  ./gradlew seed            (uses MARKET_DB_URL, default 50k listings)
 *       MARKET_SEED_COUNT=5000 ./gradlew seed
 */
const val SEED = 20260820L

data class PlantedFraud(
    val listingId: Long,
    val pattern: String,
    val evidence: String,
)

class Seeder(private val ds: DataSource, private val rng: Random = Random(SEED)) {

    private val planted = mutableListOf<PlantedFraud>()
    private val now: Instant = Instant.parse("2026-08-20T00:00:00Z")

    fun run(count: Int): List<PlantedFraud> {
        ds.use { c ->
            c.autoCommit = false
            c.createStatement().use { it.execute("truncate contact_messages, photos, price_history, listings, sellers, vin_records restart identity cascade") }

            val sellerIds = insertSellers(c, count / 12)
            val listings = (1..count).map { draft(sellerIds) }

            // Fraud is applied to drafts before they are written, so the planted
            // rows are indistinguishable from the rest at the database level.
            applyRollbacks(listings)
            applyConcealedDamage(listings)
            applyPriceOutliers(listings)
            val dupes = buildDuplicates(listings, sellerIds)

            insertListings(c, listings + dupes)
            c.commit()
        }
        return planted
    }

    // ── drafts ────────────────────────────────────────────────────────────

    private data class Draft(
        var sellerId: Long,
        var vin: String?,
        val spec: ModelSpec,
        val variant: String,
        val year: Int,
        val firstReg: LocalDate,
        var mileageKm: Int,
        var priceEur: Int,
        val fuel: String,
        val gearbox: String,
        val engineCode: String,
        val powerHp: Int,
        var serviceStamps: Int?,
        var damaged: Boolean,
        var description: String,
        val postedAt: Instant,
        val status: String,
        var photos: List<String>,
        var history: MutableList<Triple<Int, Int, Instant>> = mutableListOf(), // price, mileage, at
        var id: Long = 0,
        var truthfulMileage: Int = 0,
    )

    private val weighted: List<ModelSpec> = CATALOGUE.flatMap { s -> List(s.weight) { s } }

    private fun draft(sellerIds: List<Long>): Draft {
        val spec = weighted.random(rng)
        val year = (2010..2022).random(rng)
        val age = 2026 - year
        val firstReg = LocalDate.of(year, (1..12).random(rng), (1..28).random(rng))
        val kmPerYear = rng.nextInt(9_000, 28_000)
        val mileage = (kmPerYear * age + rng.nextInt(-8_000, 8_000)).coerceAtLeast(3_000)
        val fuel = spec.fuels.random(rng)
        val price = priceFor(spec, age, mileage)
        val posted = now.minus(rng.nextLong(0, 60), ChronoUnit.DAYS).minusSeconds(rng.nextLong(0, 86_400))
        val sellerId = sellerIds.random(rng)
        val isDealer = sellerId % 4L == 0L

        val d = Draft(
            sellerId = sellerId,
            vin = if (rng.nextInt(100) < if (isDealer) 85 else 45) vin(spec, year) else null,
            spec = spec,
            variant = spec.variants.random(rng),
            year = year,
            firstReg = firstReg,
            mileageKm = mileage,
            priceEur = price,
            fuel = fuel,
            gearbox = if (rng.nextInt(100) < 55) "manual" else "automatic",
            engineCode = spec.engineCodes.random(rng),
            powerHp = spec.powers.random(rng),
            serviceStamps = if (rng.nextInt(100) < 70) (age * rng.nextInt(0, 2) + rng.nextInt(0, 4)) else null,
            damaged = rng.nextInt(100) < 6,
            description = if (isDealer) Descriptions.dealer(rng) else Descriptions.honest(rng, mileage),
            postedAt = posted,
            status = if (rng.nextInt(100) < 8) "sold" else "active",
            photos = photos(spec, rng.nextInt(3, 10)),
        )
        d.truthfulMileage = mileage
        d.history = priceHistory(d)
        return d
    }

    private fun priceFor(spec: ModelSpec, age: Int, mileage: Int): Int {
        val depreciated = spec.newPriceEur * 0.86.pow(age)
        val mileageFactor = 1.0 - (mileage / 320_000.0) * 0.38
        val noise = rng.nextDouble(0.90, 1.12)
        return (depreciated * mileageFactor * noise).roundToInt().coerceAtLeast(900)
    }

    /** One to four earlier adverts for the same car, each with its own mileage. */
    private fun priceHistory(d: Draft): MutableList<Triple<Int, Int, Instant>> {
        val n = rng.nextInt(1, 5)
        val out = mutableListOf<Triple<Int, Int, Instant>>()
        var price = (d.priceEur * rng.nextDouble(1.02, 1.18)).roundToInt()
        var mileage = (d.mileageKm * rng.nextDouble(0.90, 0.99)).roundToInt()
        var at = d.postedAt.minus(rng.nextLong(40, 220), ChronoUnit.DAYS)
        repeat(n) {
            out += Triple(price, mileage, at)
            price = (price * rng.nextDouble(0.94, 0.995)).roundToInt()
            mileage = (mileage * rng.nextDouble(1.005, 1.04)).roundToInt().coerceAtMost(d.mileageKm)
            at = at.plus(rng.nextLong(20, 70), ChronoUnit.DAYS)
        }
        out += Triple(d.priceEur, d.mileageKm, d.postedAt)
        return out
    }

    private fun vin(spec: ModelSpec, year: Int): String {
        val wmi = when (spec.make) {
            "BMW" -> "WBA"; "Audi" -> "WAU"; "Volkswagen" -> "WVW"; "Mercedes-Benz" -> "WDD"
            "Skoda" -> "TMB"; "Seat" -> "VSS"; "Volvo" -> "YV1"; "Toyota" -> "SB1"
            "Honda" -> "SHH"; "Mazda" -> "JMZ"; "Nissan" -> "SJN"; "Renault" -> "VF1"
            "Peugeot" -> "VF3"; "Opel" -> "W0L"; "Ford" -> "WF0"; "Hyundai" -> "TMA"
            else -> "XXX"
        }
        val alphabet = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789"
        val body = (1..13).map { alphabet.random(rng) }.joinToString("")
        return wmi + body
    }

    private fun photos(spec: ModelSpec, n: Int): List<String> {
        val angles = listOf("front", "rear", "side", "interior", "dashboard", "engine", "boot", "wheels", "seats")
        val slug = "${spec.make}-${spec.model}".lowercase().replace(' ', '-')
        return angles.shuffled(rng).take(n).map { "https://cdn.example/$slug/$it.jpg" }
    }

    // ── fraud ─────────────────────────────────────────────────────────────

    /**
     * Odometer rollback. The advertised reading is cut, but the car's own earlier
     * adverts still show the higher figure, and the service stamp count no longer
     * fits. Findable only by comparing a listing against its own history.
     */
    private fun applyRollbacks(all: List<Draft>) {
        candidates(all, 20).forEach { d ->
            val real = d.mileageKm
            val rolled = (real * rng.nextDouble(0.42, 0.62)).roundToInt()
            d.mileageKm = rolled
            d.serviceStamps = ((real / 25_000.0).roundToInt()).coerceAtLeast(3)
            // The price stays where the true mileage put it: cheap for what it claims to be.
            d.history.add(Triple((d.priceEur * 1.02).roundToInt(), real, d.postedAt.minus(90, ChronoUnit.DAYS)))
            d.history.sortBy { it.third }
            d.truthfulMileage = real
            mark(d, "odometer_rollback",
                "advertised ${rolled}km; earlier advert for the same car shows ${real}km, " +
                    "and ${d.serviceStamps} service stamps imply roughly ${real}km")
        }
    }

    /** Flood or structural damage described as maintenance, with `damaged` false. */
    private fun applyConcealedDamage(all: List<Draft>) {
        candidates(all, 12).forEach { d ->
            val (text, kind) = Descriptions.concealed(rng)
            d.description = text
            d.damaged = false
            d.priceEur = (d.priceEur * rng.nextDouble(0.72, 0.88)).roundToInt()
            mark(d, "concealed_$kind",
                "damaged flag is false; description describes $kind damage; " +
                    "priced below peers with no stated reason")
        }
    }

    /**
     * Two kinds of outlier, deliberately mixed: some are misrepresentation, some
     * are simply a good car. A classifier that flags every outlier fails here.
     */
    private fun applyPriceOutliers(all: List<Draft>) {
        candidates(all, 15).forEach { d ->
            d.priceEur = (d.priceEur * rng.nextDouble(0.48, 0.62)).roundToInt()
            d.description = "Cena do negocjacji, pilna sprzedaż. " + d.description
            mark(d, "unexplained_price_outlier",
                "priced ${"%.0f".format((1 - d.priceEur.toDouble() / priceFor(d.spec, 2026 - d.year, d.mileageKm)) * 100)}% " +
                    "below the peer estimate with no stated reason")
        }
        // Legitimate outliers: expensive, and correctly so. Not marked as fraud.
        candidates(all, 10).forEach { d ->
            d.mileageKm = (d.mileageKm * 0.35).roundToInt().coerceAtLeast(9_000)
            d.priceEur = (d.priceEur * rng.nextDouble(1.35, 1.6)).roundToInt()
            d.serviceStamps = (2026 - d.year).coerceAtLeast(1)
            d.description = "Pierwszy właściciel, pełna historia serwisowa w ASO, garażowany, " +
                "przebieg potwierdzony fakturami serwisowymi."
            d.truthfulMileage = d.mileageKm
        }
    }

    /** The same physical car advertised twice, by different sellers, in different cities. */
    private fun buildDuplicates(all: List<Draft>, sellerIds: List<Long>): List<Draft> {
        return candidates(all, 15).map { original ->
            val copy = original.copy(
                sellerId = sellerIds.filter { it != original.sellerId }.random(rng),
                priceEur = (original.priceEur * rng.nextDouble(1.03, 1.12)).roundToInt(),
                description = Descriptions.honest(rng, original.mileageKm),
                history = mutableListOf(),
            )
            copy.history = mutableListOf(Triple(copy.priceEur, copy.mileageKm, copy.postedAt))
            copy.photos = original.photos              // identical photos: the giveaway
            copy.vin = original.vin                    // and the VIN, when there is one
            duplicatesOf += original to copy
            copy
        }
    }

    private val duplicatesOf = mutableListOf<Pair<Draft, Draft>>()

    private val used = mutableSetOf<Draft>()
    private fun candidates(all: List<Draft>, n: Int): List<Draft> {
        val pool = all.filter { it !in used && it.status == "active" }
        return pool.shuffled(rng).take(n).also { used += it }
    }

    private fun mark(d: Draft, pattern: String, evidence: String) {
        d.pendingMarks += pattern to evidence
    }

    private val Draft.pendingMarks: MutableList<Pair<String, String>>
        get() = marks.getOrPut(this) { mutableListOf() }
    private val marks = mutableMapOf<Draft, MutableList<Pair<String, String>>>()

    // ── writing ───────────────────────────────────────────────────────────

    private fun insertSellers(c: java.sql.Connection, n: Int): List<Long> {
        val ids = mutableListOf<Long>()
        c.prepareStatement(
            "insert into sellers (kind, display_name, city, voivodeship, joined_at) values (?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS
        ).use { st ->
            repeat(n.coerceAtLeast(50)) { i ->
                val dealer = i % 4 == 3
                val (city, voi) = CITIES.random(rng)
                st.setString(1, if (dealer) "dealer" else "private")
                st.setString(2, if (dealer) "${DEALER_NAMES.random(rng)} $city"
                               else "${FIRST_NAMES.random(rng)} ${('A'..'Z').random(rng)}.")
                st.setString(3, city)
                st.setString(4, voi)
                st.setTimestamp(5, Timestamp.from(now.minus(rng.nextLong(60, 2200), ChronoUnit.DAYS)))
                st.addBatch()
            }
            st.executeBatch()
            st.generatedKeys.use { rs -> while (rs.next()) ids += rs.getLong(1) }
        }
        return ids
    }

    private fun insertListings(c: java.sql.Connection, drafts: List<Draft>) {
        val sql = """
            insert into listings (seller_id, vin, make, model, variant, year, first_registration,
                mileage_km, price_eur, fuel, gearbox, body, engine_code, power_hp, service_stamps,
                damaged, description, posted_at, updated_at, sold_at, status)
            values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """.trimIndent()

        c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { st ->
            drafts.forEach { d ->
                st.setLong(1, d.sellerId)
                st.setString(2, d.vin)
                st.setString(3, d.spec.make)
                st.setString(4, d.spec.model)
                st.setString(5, d.variant)
                st.setInt(6, d.year)
                st.setDate(7, java.sql.Date.valueOf(d.firstReg))
                st.setInt(8, d.mileageKm)
                st.setInt(9, d.priceEur)
                st.setString(10, d.fuel)
                st.setString(11, d.gearbox)
                st.setString(12, d.spec.body)
                st.setString(13, d.engineCode)
                st.setInt(14, d.powerHp)
                if (d.serviceStamps == null) st.setNull(15, java.sql.Types.INTEGER) else st.setInt(15, d.serviceStamps!!)
                st.setBoolean(16, d.damaged)
                st.setString(17, d.description)
                st.setTimestamp(18, Timestamp.from(d.postedAt))
                st.setTimestamp(19, Timestamp.from(d.postedAt))
                if (d.status == "sold") st.setTimestamp(20, Timestamp.from(d.postedAt.plus(rng.nextLong(1, 30), ChronoUnit.DAYS)))
                else st.setNull(20, java.sql.Types.TIMESTAMP)
                st.setString(21, d.status)
                st.addBatch()
            }
            st.executeBatch()
            st.generatedKeys.use { rs ->
                var i = 0
                while (rs.next()) drafts[i++].id = rs.getLong(1)
            }
        }

        c.prepareStatement("insert into price_history (listing_id, price_eur, mileage_km, observed_at) values (?,?,?,?)").use { st ->
            drafts.forEach { d ->
                d.history.forEach { (p, m, at) ->
                    st.setLong(1, d.id); st.setInt(2, p); st.setInt(3, m)
                    st.setTimestamp(4, Timestamp.from(at)); st.addBatch()
                }
            }
            st.executeBatch()
        }

        c.prepareStatement("insert into photos (listing_id, url, position) values (?,?,?)").use { st ->
            drafts.forEach { d ->
                d.photos.forEachIndexed { i, url ->
                    st.setLong(1, d.id); st.setString(2, url); st.setInt(3, i); st.addBatch()
                }
            }
            st.executeBatch()
        }

        c.prepareStatement(
            "insert into vin_records (vin, make, model, year, engine_code, factory_options, manufactured_in) " +
                "values (?,?,?,?,?,?,?) on conflict (vin) do nothing"
        ).use { st ->
            // The decoder knows about 80% of VINs. Real ones do not cover everything either.
            drafts.filter { it.vin != null && rng.nextInt(100) < 80 }.forEach { d ->
                st.setString(1, d.vin)
                st.setString(2, d.spec.make); st.setString(3, d.spec.model); st.setInt(4, d.year)
                st.setString(5, d.engineCode)
                st.setArray(6, c.createArrayOf("text", listOf("Klimatyzacja", "Nawigacja", "Czujniki parkowania",
                    "Tempomat", "Podgrzewane fotele").shuffled(rng).take(rng.nextInt(0, 4)).toTypedArray()))
                st.setString(7, listOf("Wolfsburg", "Munich", "Ingolstadt", "Mladá Boleslav", "Ghent", "Valencia").random(rng))
                st.addBatch()
            }
            st.executeBatch()
        }

        // Ground truth, resolved now that ids exist.
        marks.forEach { (d, list) ->
            list.forEach { (pattern, evidence) -> planted += PlantedFraud(d.id, pattern, evidence) }
        }
        duplicatesOf.forEach { (a, b) ->
            planted += PlantedFraud(b.id, "duplicate_listing",
                "same physical car as listing ${a.id}: identical photo set" +
                    (if (a.vin != null) " and identical VIN ${a.vin}" else ", VIN absent on both") +
                    ", different seller and city")
        }
    }
}

fun main() {
    val config = Config.fromEnv()
    val ds = dataSource(config)
    migrate(ds)

    val count = (System.getenv("MARKET_SEED_COUNT") ?: "50000").toInt()
    println("seeding $count listings (rng seed $SEED)…")
    val started = System.currentTimeMillis()
    val planted = Seeder(ds).run(count)
    val secs = (System.currentTimeMillis() - started) / 1000.0

    val out = java.io.File("seed-truth.json")
    out.writeText(buildString {
        append("{\n  \"rng_seed\": $SEED,\n  \"listing_count\": $count,\n  \"planted\": [\n")
        append(planted.joinToString(",\n") { p ->
            """    {"listing_id": ${p.listingId}, "pattern": "${p.pattern}", "evidence": ${jsonString(p.evidence)}}"""
        })
        append("\n  ]\n}\n")
    })

    println("done in ${"%.1f".format(secs)}s — ${planted.size} planted rows written to ${out.absolutePath}")
    println(planted.groupingBy { it.pattern }.eachCount().toSortedMap().map { "  ${it.key}: ${it.value}" }.joinToString("\n"))
    println("\nDo not open that file again until chapter 25.")
}

private fun jsonString(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
