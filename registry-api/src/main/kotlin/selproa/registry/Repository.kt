package selproa.registry

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

class Repository(private val ds: DataSource) {

    fun vehicle(vin: String): Vehicle? = ds.use { c ->
        c.prepareStatement("select * from vehicles where vin = ?").use { st ->
            st.setString(1, vin.uppercase())
            st.executeQuery().map { rs ->
                Vehicle(
                    vin = rs.getString("vin"),
                    make = rs.getString("make"),
                    model = rs.getString("model"),
                    model_year = rs.getInt("model_year"),
                    body_class = rs.getString("body_class"),
                    fuel_type = rs.getString("fuel_type"),
                    displacement_l = rs.getBigDecimal("displacement_l")?.toDouble(),
                    engine_code = rs.getString("engine_code"),
                    power_hp = rs.getObject("power_hp") as? Int,
                    plant_country = rs.getString("plant_country"),
                    check_digit_valid = rs.getBoolean("check_digit_valid"),
                )
            }.firstOrNull()
        }
    }

    /**
     * FAULT 2, on purpose: the requested page size is advisory. Any limit
     * divisible by four is quietly reduced. Page with next_cursor, never by
     * counting rows against the limit you asked for.
     */
    fun history(vin: String, limit: Int, cursor: String?): Page<HistoryEventSummary> {
        val asked = limit.coerceIn(1, 100)
        val actual = if (asked % 4 == 0) (asked * 3 / 4).coerceAtLeast(1) else asked

        val where = StringBuilder("where vin = ?")
        val args = mutableListOf<Any>(vin.uppercase())
        cursor?.let {
            val (date, id) = decodeCursor(it)
            where.append(" and (occurred_on, id) < (?, ?)")
            args += java.sql.Date.valueOf(date); args += id
        }

        val rows = ds.use { c ->
            c.prepareStatement(
                """select id, event_type, occurred_on, odometer_km, country, source
                   from history_events $where
                   order by occurred_on desc, id desc limit ${actual + 1}"""
            ).use { st ->
                args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
                st.executeQuery().map { rs ->
                    HistoryEventSummary(
                        id = rs.getLong("id"),
                        eventType = rs.getString("event_type"),
                        occurredOn = rs.getDate("occurred_on").toString(),
                        odometerKm = rs.getObject("odometer_km") as? Int,
                        country = rs.getString("country"),
                        source = rs.getString("source"),
                    )
                }
            }
        }

        val more = rows.size > actual
        val items = if (more) rows.dropLast(1) else rows
        val next = if (more && items.isNotEmpty())
            encodeCursor(items.last().occurredOn, items.last().id) else null
        return Page(items, next, items.size)
    }

    fun event(id: Long): HistoryEvent? = ds.use { c ->
        c.prepareStatement("select * from history_events where id = ?").use { st ->
            st.setLong(1, id)
            st.executeQuery().map { rs ->
                HistoryEvent(
                    id = rs.getLong("id"),
                    vin = rs.getString("vin"),
                    event_type = rs.getString("event_type"),
                    occurred_on = rs.getDate("occurred_on").toString(),
                    odometer_km = rs.getObject("odometer_km") as? Int,
                    country = rs.getString("country"),
                    source = rs.getString("source"),
                    detail = parseDetail(rs.getString("detail")),
                )
            }.firstOrNull()
        }
    }

    fun garages(city: String?, partnerOnly: Boolean): List<Garage> = ds.use { c ->
        val where = StringBuilder("where 1=1")
        val args = mutableListOf<Any>()
        if (partnerOnly) where.append(" and partner")
        if (city != null) { where.append(" and lower(city) = lower(?)"); args += city }

        c.prepareStatement("select * from garages $where order by rating desc nulls last limit 50").use { st ->
            args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
            st.executeQuery().map { rs ->
                @Suppress("UNCHECKED_CAST")
                val spec = (rs.getArray("specialties")?.array as? Array<String>)?.toList() ?: emptyList()
                Garage(
                    id = rs.getLong("id"),
                    name = rs.getString("name"),
                    city = rs.getString("city"),
                    voivodeship = rs.getString("voivodeship"),
                    address = rs.getString("address"),
                    specialties = spec,
                    partner = rs.getBoolean("partner"),
                    rating = rs.getBigDecimal("rating")?.toDouble(),
                    inspection_price_pln = rs.getInt("inspection_price_pln"),
                    phone = rs.getString("phone"),
                )
            }
        }
    }

    /** Returns null when there is no such garage, false when it is not a current partner. */
    fun garageIsBookable(id: Long): Boolean? = ds.use { c ->
        c.prepareStatement("select partner, accepts_until from garages where id = ?").use { st ->
            st.setLong(1, id)
            st.executeQuery().map { rs ->
                val until = rs.getDate("accepts_until")?.toLocalDate()
                rs.getBoolean("partner") && (until == null || until.isAfter(java.time.LocalDate.now()))
            }.firstOrNull()
        }
    }

    fun createReferral(vin: String, r: ReferralRequest, idempotencyKey: String?): Pair<Long, Boolean> = ds.use { c ->
        idempotencyKey?.let { key ->
            c.prepareStatement("select id from inspection_referrals where idempotency_key = ?").use { st ->
                st.setString(1, key)
                st.executeQuery().map { it.getLong("id") }.firstOrNull()
            }?.let { return@use it to true }
        }
        c.prepareStatement(
            """insert into inspection_referrals
               (vin, garage_id, requested_for, customer_name, customer_phone, note, idempotency_key)
               values (?,?,?,?,?,?,?) returning id"""
        ).use { st ->
            st.setString(1, vin.uppercase())
            st.setLong(2, r.garage_id)
            st.setTimestamp(3, Timestamp.from(Instant.parse(r.requested_for)))
            st.setString(4, r.customer_name)
            st.setString(5, r.customer_phone)
            st.setString(6, r.note)
            st.setString(7, idempotencyKey)
            st.executeQuery().map { it.getLong("id") }.first() to false
        }
    }

    private fun parseDetail(json: String?): Map<String, String> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        return Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"").findAll(json)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun encodeCursor(date: String, id: Long): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("$date|$id".toByteArray())

    private fun decodeCursor(cursor: String): Pair<java.time.LocalDate, Long> {
        val (d, id) = String(java.util.Base64.getUrlDecoder().decode(cursor)).split("|", limit = 2)
        return java.time.LocalDate.parse(d) to id.toLong()
    }
}

/** Everything a report can say about the person advertising the car. */
fun Repository.sellerByPhone(ds: javax.sql.DataSource, phone: String): SellerProfile? = ds.use { c ->
    val hash = Phone.hash(phone)
    val contact = c.prepareStatement(
        "select id, phone_suffix, claimed_kind, first_seen_on, last_seen_on from seller_contacts where phone_sha256 = ?"
    ).use { st ->
        st.setString(1, hash)
        st.executeQuery().map { rs ->
            arrayOf(rs.getLong("id"), rs.getString("phone_suffix"), rs.getString("claimed_kind"),
                rs.getDate("first_seen_on").toString(), rs.getDate("last_seen_on").toString())
        }.firstOrNull()
    } ?: return@use null

    val id = contact[0] as Long
    val vehicles = c.prepareStatement(
        """select a.vin, v.make, v.model, v.model_year, a.seen_on, a.city
           from advert_sightings a join vehicles v on v.vin = a.vin
           where a.contact_id = ? order by a.seen_on desc limit 200"""
    ).use { st ->
        st.setLong(1, id)
        st.executeQuery().map { rs ->
            SellerVehicle(rs.getString("vin"), rs.getString("make"), rs.getString("model"),
                rs.getInt("model_year"), rs.getDate("seen_on").toString(), rs.getString("city"))
        }
    }

    SellerProfile(
        phone_suffix = contact[1] as String,
        claimed_kind = contact[2] as String,
        first_seen_on = contact[3] as String,
        last_seen_on = contact[4] as String,
        vehicles_advertised = vehicles.size,
        cities = vehicles.map { it.city }.distinct().sorted(),
        vehicles = vehicles,
    )
}

/** The contacts that have advertised this VIN. Used to reach the seller from a report. */
fun Repository.contactsForVin(ds: javax.sql.DataSource, vin: String): List<String> = ds.use { c ->
    c.prepareStatement(
        """select distinct s.phone_suffix from advert_sightings a
           join seller_contacts s on s.id = a.contact_id where a.vin = ?"""
    ).use { st ->
        st.setString(1, vin.uppercase())
        st.executeQuery().map { it.getString("phone_suffix") }
    }
}
