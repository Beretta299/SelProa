package selproa.registry

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val appLog = LoggerFactory.getLogger("registry-api")

fun main() {
    val config = Config.fromEnv()
    val ds = dataSource(config)
    migrate(ds)
    appLog.info("migrated; starting on :{}", config.port)

    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(Repository(ds))
    }.start(wait = true)
}

fun Application.module(repo: Repository) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
    }
    install(CallLogging)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            appLog.error("unhandled", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("internal_error"))
        }
    }

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }

        get("/vehicles/{vin}") {
            val vin = call.parameters["vin"].orEmpty()
            val v = repo.vehicle(vin)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiError("vin_not_found"))
            call.respond(v)
        }

        get("/vehicles/{vin}/history") {
            val vin = call.parameters["vin"].orEmpty()
            if (repo.vehicle(vin) == null)
                return@get call.respond(HttpStatusCode.NotFound, ApiError("vin_not_found"))
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            call.respond(repo.history(vin, limit, call.request.queryParameters["cursor"]))
        }

        get("/events/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiError("bad_id"))
            val e = repo.event(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiError("not_found"))
            call.respond(e)
        }

        get("/garages") {
            val city = call.request.queryParameters["city"]
            val partnerOnly = call.request.queryParameters["partner"] != "false"
            call.respond(repo.garages(city, partnerOnly))
        }

        /**
         * The consequential write: tells a real business a real customer's name
         * and number, and commits them to an appointment.
         *
         * FAULT 3, on purpose: when the garage is no longer an active partner,
         * this answers 200 with an empty body rather than an error. A client that
         * checks only the status code will tell its user the inspection is booked.
         * Nobody is expecting them.
         */
        post("/vehicles/{vin}/referrals") {
            val vin = call.parameters["vin"].orEmpty()
            if (repo.vehicle(vin) == null)
                return@post call.respond(HttpStatusCode.NotFound, ApiError("vin_not_found"))

            val req = call.receive<ReferralRequest>()
            when (repo.garageIsBookable(req.garage_id)) {
                null -> return@post call.respond(HttpStatusCode.NotFound, ApiError("garage_not_found"))
                true -> Unit
                false -> {
                    call.respondText("", ContentType.Application.Json, HttpStatusCode.OK)
                    return@post
                }
            }

            val (id, duplicate) = repo.createReferral(vin, req, call.request.headers["Idempotency-Key"])
            call.respond(ReferralAccepted(id, "sent", duplicate))
        }
    }
}
