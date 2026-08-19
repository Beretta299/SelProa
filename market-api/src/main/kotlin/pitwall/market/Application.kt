package pitwall.market

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

private val appLog = LoggerFactory.getLogger("market-api")

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
        json(Json { prettyPrint = false; ignoreUnknownKeys = true; encodeDefaults = true })
    }
    install(CallLogging)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            appLog.error("unhandled", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("internal_error"))
        }
    }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        get("/listings") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val cursor = call.request.queryParameters["cursor"]
            val make = call.request.queryParameters["make"]
            val model = call.request.queryParameters["model"]
            call.respond(repo.listings(limit, cursor, make, model))
        }

        get("/listings/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiError("bad_id"))
            val listing = repo.listing(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiError("not_found"))
            call.respond(listing)
        }

        get("/listings/{id}/price-history") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiError("bad_id"))
            call.respond(repo.priceHistory(id))
        }

        get("/vin/{vin}") {
            val vin = call.parameters["vin"].orEmpty()
            val record = repo.vin(vin)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiError("vin_not_found"))
            call.respond(record)
        }

        /**
         * FAULT 3, on purpose: when the listing has already sold, this responds
         * 200 with an empty body rather than an error. A client that checks only
         * the status code will tell its user the message was sent.
         *
         * This is the single most important fault in the service. See docs/faults.md.
         */
        post("/listings/{id}/contact") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiError("bad_id"))

            when (repo.statusOf(id)) {
                null -> return@post call.respond(HttpStatusCode.NotFound, ApiError("not_found"))
                "active" -> Unit
                else -> {
                    // Intentionally: 200, no body, no explanation.
                    call.respondText("", ContentType.Application.Json, HttpStatusCode.OK)
                    return@post
                }
            }

            val body = call.receiveText()
            val key = call.request.headers["Idempotency-Key"]
            val created = repo.recordContact(id, body, key)
            call.respond(HttpStatusCode.OK, mapOf("delivered" to true, "duplicate" to !created))
        }
    }
}
