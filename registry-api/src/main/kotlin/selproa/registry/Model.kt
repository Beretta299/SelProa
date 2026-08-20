package selproa.registry

import kotlinx.serialization.Serializable

/**
 * Wire shapes.
 *
 * FAULT 1, on purpose: the history list calls the discriminator `eventType`,
 * the single-event shape calls it `event_type`, and the vehicle summary says
 * `modelYear` where the detail says `model_year`. Real registries grew in
 * layers written by different people. Normalise at your own boundary.
 */
@Serializable
data class VehicleSummary(
    val vin: String,
    val make: String,
    val model: String,
    val modelYear: Int,
    val fuelType: String?,
    val checkDigitValid: Boolean,
    val eventCount: Int,
)

@Serializable
data class Vehicle(
    val vin: String,
    val make: String,
    val model: String,
    val model_year: Int,
    val body_class: String?,
    val fuel_type: String?,
    val displacement_l: Double?,
    val engine_code: String?,
    val power_hp: Int?,
    val plant_country: String?,
    val check_digit_valid: Boolean,
)

@Serializable
data class HistoryEventSummary(
    val id: Long,
    val eventType: String,
    val occurredOn: String,
    val odometerKm: Int?,
    val country: String,
    val source: String,
)

@Serializable
data class HistoryEvent(
    val id: Long,
    val vin: String,
    val event_type: String,
    val occurred_on: String,
    val odometer_km: Int?,
    val country: String,
    val source: String,
    val detail: Map<String, String>,
)

@Serializable
data class Garage(
    val id: Long,
    val name: String,
    val city: String,
    val voivodeship: String,
    val address: String,
    val specialties: List<String>,
    val partner: Boolean,
    val rating: Double?,
    val inspection_price_pln: Int,
    val phone: String,
)

@Serializable
data class Page<T>(val items: List<T>, val next_cursor: String?, val page_size: Int)

@Serializable
data class ApiError(val error: String, val detail: String? = null)

@Serializable
data class ReferralRequest(
    val garage_id: Long,
    val requested_for: String,
    val customer_name: String,
    val customer_phone: String,
    val note: String? = null,
)

@Serializable
data class ReferralAccepted(val referral_id: Long, val status: String, val duplicate: Boolean)

@Serializable
data class SellerProfile(
    val phone_suffix: String,
    val claimed_kind: String,
    val first_seen_on: String,
    val last_seen_on: String,
    val vehicles_advertised: Int,
    val cities: List<String>,
    val vehicles: List<SellerVehicle>,
)

@Serializable
data class SellerVehicle(
    val vin: String,
    val make: String,
    val model: String,
    val model_year: Int,
    val seen_on: String,
    val city: String,
)
