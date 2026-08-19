package pitwall.market

import kotlinx.serialization.Serializable
import kotlin.random.Random
import javax.sql.DataSource

@Serializable
data class FeedEvent(
    val type: String,           // new_listing | price_change | sold
    val listingId: Long,        // camelCase here too -- see docs/faults.md
    val make: String,
    val model: String,
    val year: Int,
    val price_eur: Int,
    val previous_price_eur: Int? = null,
    val at: String,
)

/**
 * The live feed a real marketplace pushes to integrators. Synthesised from rows
 * that already exist, because a mock does not need genuine market movement -- it
 * needs something arriving at an unpredictable interval that a consumer must
 * handle without dropping messages.
 */
class Feed(private val ds: DataSource) {

    fun next(rng: Random): FeedEvent? = ds.use { c ->
        c.prepareStatement(
            """select id, make, model, year, price_eur
               from listings where status = 'active'
               offset floor(random() * greatest((select count(*) from listings where status='active'), 1))
               limit 1"""
        ).use { st ->
            st.executeQuery().map { rs ->
                val price = rs.getInt("price_eur")
                val type = when (rng.nextInt(100)) {
                    in 0..54 -> "price_change"
                    in 55..89 -> "new_listing"
                    else -> "sold"
                }
                val newPrice = if (type == "price_change")
                    (price * rng.nextDouble(0.93, 0.99)).toInt() else price
                FeedEvent(
                    type = type,
                    listingId = rs.getLong("id"),
                    make = rs.getString("make"),
                    model = rs.getString("model"),
                    year = rs.getInt("year"),
                    price_eur = newPrice,
                    previous_price_eur = if (type == "price_change") price else null,
                    at = java.time.Instant.now().toString(),
                )
            }.firstOrNull()
        }
    }
}
