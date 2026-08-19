package pitwall.market

import kotlinx.serialization.Serializable

/**
 * Wire shapes.
 *
 * Note the naming: [Listing] uses snake_case for most fields but [ListingSummary]
 * uses camelCase for the identifier. That inconsistency is deliberate -- it is the
 * first of the three faults this service exists to have. Anything consuming it has
 * to normalise at its own boundary.
 */
@Serializable
data class ListingSummary(
    val listingId: Long,
    val make: String,
    val model: String,
    val year: Int,
    val mileage_km: Int,
    val price_eur: Int,
    val fuel: String,
    val gearbox: String,
    val posted_at: String,
)

@Serializable
data class Listing(
    val listing_id: Long,
    val vin: String?,
    val make: String,
    val model: String,
    val variant: String?,
    val year: Int,
    val first_registration: String?,
    val mileage_km: Int,
    val price_eur: Int,
    val fuel: String,
    val gearbox: String,
    val body: String,
    val engine_code: String?,
    val power_hp: Int?,
    val service_stamps: Int?,
    val damaged: Boolean,
    val description: String,
    val seller: Seller,
    val photos: List<String>,
    val posted_at: String,
    val status: String,
)

@Serializable
data class Seller(
    val id: Long,
    val kind: String,
    val display_name: String,
    val city: String,
    val voivodeship: String,
)

@Serializable
data class Page<T>(
    val items: List<T>,
    val next_cursor: String?,
    val page_size: Int,
)

@Serializable
data class PricePoint(val price_eur: Int, val observed_at: String)

@Serializable
data class VinRecord(
    val vin: String,
    val make: String,
    val model: String,
    val year: Int,
    val engine_code: String?,
    val factory_options: List<String>,
    val manufactured_in: String?,
)

@Serializable
data class ApiError(val error: String, val detail: String? = null)
