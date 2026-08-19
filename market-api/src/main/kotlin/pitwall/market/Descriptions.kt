package pitwall.market

import kotlin.random.Random

/**
 * Listing prose, written the way tired people actually write it at 08:00:
 * inconsistent capitalisation, abbreviations, mixed Polish and English.
 *
 * The [concealing] pool is the important one. Those phrases describe flood or
 * accident damage in language a seller uses when they would rather you did not
 * notice, on listings whose `damaged` flag says false.
 */
object Descriptions {

    private val honestSpecific = listOf(
        "Serwisowany w ASO, druga ręka, komplet kluczy. Rozrząd wymieniony przy %d tys. km.",
        "Kupiony w polskim salonie, bezwypadkowy. Ostatni serwis %d tys. km, faktura w załączeniu.",
        "Full service history. Timing belt done at %d000 km, new brake discs front and rear.",
        "Auto z gwarancją przebiegu. Opony zimowe w cenie. Przegląd ważny do końca roku.",
        "Zadbany egzemplarz, garażowany. Klimatyzacja odgrzybiona, olej wymieniony przy %d tys.",
    )

    private val honestSparse = listOf(
        "zapraszam do ogladania", "stan dobry", "auto sprawne, jezdzi codziennie",
        "wiecej info telefonicznie", "polecam", "do negocjacji", "pilnie sprzedam",
        "auto gotowe do jazdy, mozliwa zamiana",
    )

    private val dealerBoilerplate = listOf(
        "Zapraszamy do naszego komisu. Faktura VAT 23%%, możliwość leasingu i kredytu. " +
            "Auto sprawdzone przez naszych mechaników. Gwarancja rozruchowa 3 miesiące.",
        "Samochód po pełnym przeglądzie w naszym serwisie. Przyjmujemy auta w rozliczeniu. " +
            "Transport na terenie całego kraju. Zapraszamy 7 dni w tygodniu.",
        "AUTO Z GWARANCJĄ! Sprawdzona historia pojazdu. Możliwość jazdy próbnej. " +
            "Finansowanie na miejscu, minimum formalności.",
    )

    /** Damage described so that it reads as maintenance. `damaged` stays false. */
    private val concealing = listOf(
        "Auto po niewielkim zalaniu, profesjonalnie osuszone, tapicerka wymieniona na nową." to "flood",
        "Minor water exposure during storage, fully dried and treated. No electrical issues." to "flood",
        "Silnik po regeneracji, wszystko działa jak nowe. Lakier odświeżony na dwóch elementach." to "structural",
        "Przód delikatnie muśnięty, blacharka zrobiona u dobrego fachowca, nie znać." to "structural",
        "Wymieniona poduszka kierowcy i pas bezpieczeństwa, reszta oryginalna." to "airbag",
        "Car was briefly submerged in a flooded garage, professionally restored, drives perfectly." to "flood",
        "Po kolizji z sarną, wymieniony przód, wszystko na fakturach." to "structural",
    )

    fun honest(rng: Random, mileageKm: Int): String {
        val km = mileageKm / 1000
        // Low-mileage cars exist; the service milestone has to fit inside the odometer.
        val serviceAt = if (km > 25) rng.nextInt(20, km) else km.coerceAtLeast(1)
        return if (rng.nextInt(100) < 45) honestSpecific.random(rng).format(serviceAt)
               else honestSparse.random(rng)
    }

    fun dealer(rng: Random): String = dealerBoilerplate.random(rng)

    /** Returns the description and the kind of damage it is hiding. */
    fun concealed(rng: Random): Pair<String, String> = concealing.random(rng)
}
