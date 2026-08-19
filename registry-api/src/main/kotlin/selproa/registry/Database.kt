package selproa.registry

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

fun dataSource(config: Config): DataSource {
    val hikari = HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.dbUser
        password = config.dbPassword
        maximumPoolSize = 10
        // A mock service that hangs is worse than one that fails.
        connectionTimeout = 5_000
        validate()
    }
    return HikariDataSource(hikari)
}

fun migrate(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()
}

/** Runs [block] with a connection and always returns it to the pool. */
fun <T> DataSource.use(block: (Connection) -> T): T = connection.use(block)

/** Maps every row of a result set. */
fun <T> ResultSet.map(row: (ResultSet) -> T): List<T> {
    val out = mutableListOf<T>()
    while (next()) out += row(this)
    return out
}
