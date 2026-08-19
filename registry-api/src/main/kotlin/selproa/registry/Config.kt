package selproa.registry

data class Config(
    val port: Int,
    val jdbcUrl: String,
    val dbUser: String,
    val dbPassword: String,
) {
    companion object {
        fun fromEnv(): Config = Config(
            port = env("REGISTRY_API_PORT", "8081").toInt(),
            jdbcUrl = env("REGISTRY_DB_URL", "jdbc:postgresql://localhost:5433/registry"),
            dbUser = env("REGISTRY_DB_USER", "registry"),
            dbPassword = env("REGISTRY_DB_PASSWORD", "registry"),
        )

        private fun env(key: String, default: String) = System.getenv(key) ?: default
    }
}
