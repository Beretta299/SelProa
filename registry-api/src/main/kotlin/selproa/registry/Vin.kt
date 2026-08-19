package selproa.registry

/**
 * VIN arithmetic.
 *
 * The ninth character is a checksum over the other sixteen. A fabricated VIN
 * usually fails it, which makes this the cheapest fraud signal in the product --
 * no data source required, just seventeen characters.
 */
object Vin {
    private const val ALPHABET = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789" // no I, O, Q
    private val WEIGHTS = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)

    private fun value(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        'A', 'J' -> 1; 'B', 'K', 'S' -> 2; 'C', 'L', 'T' -> 3
        'D', 'M', 'U' -> 4; 'E', 'N', 'V' -> 5; 'F', 'W' -> 6
        'G', 'P', 'X' -> 7; 'H', 'Y' -> 8; 'R', 'Z' -> 9
        else -> 0
    }

    fun checkDigit(vin: String): Char {
        val sum = vin.indices.sumOf { i -> value(vin[i]) * WEIGHTS[i] }
        val r = sum % 11
        return if (r == 10) 'X' else '0' + r
    }

    fun isValid(vin: String): Boolean =
        vin.length == 17 && vin[8] == checkDigit(vin)

    /** Builds a VIN with a correct check digit. */
    fun generate(wmi: String, rng: kotlin.random.Random): String {
        val rest = (1..14).map { ALPHABET.random(rng) }.joinToString("")
        val draft = (wmi + rest).toCharArray()
        draft[8] = '0'
        val withCd = String(draft)
        draft[8] = checkDigit(withCd)
        return String(draft)
    }

    /** Same, but the check digit is deliberately wrong. */
    fun generateInvalid(wmi: String, rng: kotlin.random.Random): String {
        val good = generate(wmi, rng)
        val wrong = ALPHABET.filter { it != good[8] && it != 'X' }.random(rng)
        return good.substring(0, 8) + wrong + good.substring(9)
    }
}
