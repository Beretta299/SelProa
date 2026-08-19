package pitwall.market

data class Config(
    val port: Int,
    val jdbcUrl: String,
    val dbUser: String,
    val dbPassword: String,
) {
    companion object {
        fun fromEnv(): Config = Config(
            port = env("MARKET_API_PORT", "8081").toInt(),
            jdbcUrl = env("MARKET_DB_URL", "jdbc:postgresql://localhost:5432/market"),
            dbUser = env("MARKET_DB_USER", "market"),
            dbPassword = env("MARKET_DB_PASSWORD", "market"),
        )

        private fun env(key: String, default: String) = System.getenv(key) ?: default
    }
}
