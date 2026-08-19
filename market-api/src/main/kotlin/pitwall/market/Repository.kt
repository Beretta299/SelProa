package pitwall.market

import java.sql.ResultSet
import java.time.Instant
import javax.sql.DataSource

class Repository(private val ds: DataSource) {

    /**
     * Cursor pagination over active listings.
     *
     * FAULT 2, on purpose: the page size the caller asks for is not necessarily the
     * page size they get. Real marketplaces cap, round and occasionally ignore the
     * parameter, and a client that assumes `limit` is honoured will silently skip
     * rows. See docs/faults.md.
     */
    fun listings(limit: Int, cursor: String?, make: String?, model: String?): Page<ListingSummary> {
        val asked = limit.coerceIn(1, 100)
        val actual = driftPageSize(asked)

        val where = StringBuilder("where l.status = 'active'")
        val args = mutableListOf<Any>()
        if (make != null) { where.append(" and lower(l.make) = lower(?)"); args += make }
        if (model != null) { where.append(" and lower(l.model) = lower(?)"); args += model }
        cursor?.let {
            val (ts, id) = decodeCursor(it)
            where.append(" and (l.posted_at, l.id) < (?, ?)")
            args += java.sql.Timestamp.from(ts)
            args += id
        }

        val sql = """
            select l.id, l.make, l.model, l.year, l.mileage_km, l.price_eur,
                   l.fuel, l.gearbox, l.posted_at
            from listings l
            $where
            order by l.posted_at desc, l.id desc
            limit ${actual + 1}
        """.trimIndent()

        val rows = ds.use { c ->
            c.prepareStatement(sql).use { st ->
                args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
                st.executeQuery().map { rs ->
                    ListingSummary(
                        listingId = rs.getLong("id"),
                        make = rs.getString("make"),
                        model = rs.getString("model"),
                        year = rs.getInt("year"),
                        mileage_km = rs.getInt("mileage_km"),
                        price_eur = rs.getInt("price_eur"),
                        fuel = rs.getString("fuel"),
                        gearbox = rs.getString("gearbox"),
                        posted_at = rs.getTimestamp("posted_at").toInstant().toString(),
                    )
                }
            }
        }

        val hasMore = rows.size > actual
        val items = if (hasMore) rows.dropLast(1) else rows
        val next = if (hasMore && items.isNotEmpty()) {
            val last = items.last()
            encodeCursor(Instant.parse(last.posted_at), last.listingId)
        } else null

        return Page(items = items, next_cursor = next, page_size = items.size)
    }

    fun listing(id: Long): Listing? = ds.use { c ->
        val sql = """
            select l.*, s.id as s_id, s.kind, s.display_name, s.city, s.voivodeship
            from listings l join sellers s on s.id = l.seller_id
            where l.id = ?
        """.trimIndent()
        val listing = c.prepareStatement(sql).use { st ->
            st.setLong(1, id)
            st.executeQuery().map { rs -> rs.toListing(emptyList()) }.firstOrNull()
        } ?: return@use null

        val photos = c.prepareStatement(
            "select url from photos where listing_id = ? order by position"
        ).use { st ->
            st.setLong(1, id)
            st.executeQuery().map { it.getString("url") }
        }
        listing.copy(photos = photos)
    }

    fun priceHistory(listingId: Long): List<PricePoint> = ds.use { c ->
        c.prepareStatement(
            "select price_eur, observed_at from price_history where listing_id = ? order by observed_at"
        ).use { st ->
            st.setLong(1, listingId)
            st.executeQuery().map {
                PricePoint(it.getInt("price_eur"), it.getTimestamp("observed_at").toInstant().toString())
            }
        }
    }

    fun vin(vin: String): VinRecord? = ds.use { c ->
        c.prepareStatement("select * from vin_records where vin = ?").use { st ->
            st.setString(1, vin.uppercase())
            st.executeQuery().map { rs ->
                @Suppress("UNCHECKED_CAST")
                val opts = (rs.getArray("factory_options")?.array as? Array<String>)?.toList() ?: emptyList()
                VinRecord(
                    vin = rs.getString("vin"),
                    make = rs.getString("make"),
                    model = rs.getString("model"),
                    year = rs.getInt("year"),
                    engine_code = rs.getString("engine_code"),
                    factory_options = opts,
                    manufactured_in = rs.getString("manufactured_in"),
                )
            }.firstOrNull()
        }
    }

    /** Returns the listing's status, or null when there is no such listing. */
    fun statusOf(id: Long): String? = ds.use { c ->
        c.prepareStatement("select status from listings where id = ?").use { st ->
            st.setLong(1, id)
            st.executeQuery().map { it.getString("status") }.firstOrNull()
        }
    }

    fun recordContact(listingId: Long, body: String, idempotencyKey: String?): Boolean = ds.use { c ->
        c.prepareStatement(
            """insert into contact_messages (listing_id, body, idempotency_key)
               values (?, ?, ?) on conflict (idempotency_key) do nothing"""
        ).use { st ->
            st.setLong(1, listingId)
            st.setString(2, body)
            st.setString(3, idempotencyKey)
            st.executeUpdate() > 0
        }
    }

    private fun ResultSet.toListing(photos: List<String>) = Listing(
        listing_id = getLong("id"),
        vin = getString("vin"),
        make = getString("make"),
        model = getString("model"),
        variant = getString("variant"),
        year = getInt("year"),
        first_registration = getDate("first_registration")?.toString(),
        mileage_km = getInt("mileage_km"),
        price_eur = getInt("price_eur"),
        fuel = getString("fuel"),
        gearbox = getString("gearbox"),
        body = getString("body"),
        engine_code = getString("engine_code"),
        power_hp = getObject("power_hp") as? Int,
        service_stamps = getObject("service_stamps") as? Int,
        damaged = getBoolean("damaged"),
        description = getString("description"),
        seller = Seller(
            id = getLong("s_id"),
            kind = getString("kind"),
            display_name = getString("display_name"),
            city = getString("city"),
            voivodeship = getString("voivodeship"),
        ),
        photos = photos,
        posted_at = getTimestamp("posted_at").toInstant().toString(),
        status = getString("status"),
    )

    /** Deterministic so tests can rely on it: every fourth page size is quietly reduced. */
    private fun driftPageSize(asked: Int): Int =
        if (asked % 4 == 0) (asked * 3 / 4).coerceAtLeast(1) else asked

    private fun encodeCursor(ts: Instant, id: Long): String =
        java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("$ts|$id".toByteArray())

    private fun decodeCursor(cursor: String): Pair<Instant, Long> {
        val raw = String(java.util.Base64.getUrlDecoder().decode(cursor))
        val (ts, id) = raw.split("|", limit = 2)
        return Instant.parse(ts) to id.toLong()
    }
}
