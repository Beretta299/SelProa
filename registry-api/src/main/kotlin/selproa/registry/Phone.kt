package selproa.registry

import java.security.MessageDigest

/**
 * Phone numbers are personal data. They are matched by hash, never stored in
 * clear, so a copy of this database is not a copy of everyone's phone book.
 */
object Phone {
    /** +48 791 298 725, 791298725 and 0791-298-725 all have to reach the same row. */
    fun normalise(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 9 -> "+48$digits"                    // bare Polish number
            digits.startsWith("48") && digits.length == 11 -> "+$digits"
            digits.startsWith("0") && digits.length == 10 -> "+48${digits.drop(1)}"
            else -> "+$digits"
        }
    }

    fun hash(raw: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(normalise(raw).toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun suffix(raw: String): String = normalise(raw).takeLast(3)
}
