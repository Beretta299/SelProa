package pitwall.market

/** What the Polish used market is actually full of. */
data class ModelSpec(
    val make: String,
    val model: String,
    val variants: List<String>,
    val body: String,
    val fuels: List<String>,
    val newPriceEur: Int,
    val engineCodes: List<String>,
    val powers: IntRange,
    val weight: Int, // relative frequency on the market
)

val CATALOGUE = listOf(
    ModelSpec("Volkswagen", "Golf", listOf("1.6 TDI", "2.0 TDI", "1.4 TSI", "1.5 TSI"), "hatchback",
        listOf("diesel", "petrol"), 27000, listOf("CLHA", "CRBC", "CZCA", "DADA"), 105..150, 10),
    ModelSpec("Volkswagen", "Passat", listOf("2.0 TDI", "1.8 TSI", "2.0 TDI SCR"), "estate",
        listOf("diesel", "petrol"), 34000, listOf("CRLB", "DFGA", "CJSA"), 150..190, 8),
    ModelSpec("Skoda", "Octavia", listOf("1.6 TDI", "2.0 TDI", "1.4 TSI"), "estate",
        listOf("diesel", "petrol"), 24000, listOf("CLHA", "DFGA", "CZCA"), 110..184, 9),
    ModelSpec("Audi", "A4", listOf("2.0 TDI", "1.8 TFSI", "2.0 TDI quattro"), "estate",
        listOf("diesel", "petrol"), 40000, listOf("CNHA", "CJEB", "DETA"), 150..190, 7),
    ModelSpec("Audi", "A6", listOf("2.0 TDI", "3.0 TDI quattro"), "estate",
        listOf("diesel"), 52000, listOf("CNHA", "CDUD"), 177..272, 4),
    ModelSpec("BMW", "3 Series", listOf("318d", "320d", "320i", "330d"), "saloon",
        listOf("diesel", "petrol"), 41000, listOf("N47D20", "B47D20", "N20B20", "N57D30"), 143..258, 7),
    ModelSpec("BMW", "5 Series", listOf("520d", "530d", "525d"), "saloon",
        listOf("diesel"), 55000, listOf("N47D20", "N57D30", "B47D20"), 184..258, 4),
    ModelSpec("Mercedes-Benz", "C-Class", listOf("C 220 d", "C 200", "C 250 d"), "saloon",
        listOf("diesel", "petrol"), 44000, listOf("OM651", "M274", "OM626"), 156..204, 5),
    ModelSpec("Opel", "Astra", listOf("1.6 CDTI", "1.4 Turbo", "1.7 CDTI"), "hatchback",
        listOf("diesel", "petrol"), 21000, listOf("B16DTH", "B14XFT", "A17DTS"), 110..150, 8),
    ModelSpec("Ford", "Focus", listOf("1.5 TDCi", "1.0 EcoBoost", "1.6 TDCi"), "hatchback",
        listOf("diesel", "petrol"), 22000, listOf("XWDA", "M1DA", "T1DA"), 95..150, 7),
    ModelSpec("Toyota", "Corolla", listOf("1.6 Valvematic", "1.8 Hybrid", "1.4 D-4D"), "saloon",
        listOf("petrol", "hybrid", "diesel"), 24000, listOf("1ZR-FAE", "2ZR-FXE", "1ND-TV"), 90..136, 6),
    ModelSpec("Renault", "Megane", listOf("1.5 dCi", "1.2 TCe", "1.6 dCi"), "hatchback",
        listOf("diesel", "petrol"), 21000, listOf("K9K", "H5F", "R9M"), 90..130, 5),
    ModelSpec("Peugeot", "308", listOf("1.6 BlueHDi", "1.2 PureTech", "2.0 BlueHDi"), "hatchback",
        listOf("diesel", "petrol"), 22000, listOf("DV6", "EB2", "DW10"), 100..150, 5),
    ModelSpec("Mazda", "6", listOf("2.2 Skyactiv-D", "2.0 Skyactiv-G"), "estate",
        listOf("diesel", "petrol"), 30000, listOf("SH-VPTS", "PE-VPS"), 145..175, 4),
    ModelSpec("Hyundai", "i30", listOf("1.6 CRDi", "1.4 T-GDI"), "hatchback",
        listOf("diesel", "petrol"), 20000, listOf("D4FB", "G4LD"), 110..140, 5),
    ModelSpec("Kia", "Ceed", listOf("1.6 CRDi", "1.0 T-GDI"), "hatchback",
        listOf("diesel", "petrol"), 20000, listOf("D4FB", "G3LC"), 100..136, 5),
    ModelSpec("Nissan", "Qashqai", listOf("1.5 dCi", "1.2 DIG-T", "1.6 dCi"), "suv",
        listOf("diesel", "petrol"), 26000, listOf("K9K", "HRA2DDT", "R9M"), 110..130, 6),
    ModelSpec("Volvo", "V60", listOf("D3", "D4", "T4"), "estate",
        listOf("diesel", "petrol"), 42000, listOf("D5204T", "D4204T", "B4204T"), 150..190, 3),
    ModelSpec("Seat", "Leon", listOf("1.6 TDI", "2.0 TDI", "1.4 TSI"), "hatchback",
        listOf("diesel", "petrol"), 22000, listOf("CLHA", "CRBC", "CZCA"), 105..184, 5),
    ModelSpec("Honda", "Civic", listOf("1.6 i-DTEC", "1.5 VTEC Turbo"), "hatchback",
        listOf("diesel", "petrol"), 24000, listOf("N16A1", "L15B7"), 120..182, 4),
)

val CITIES = listOf(
    "Warszawa" to "mazowieckie", "Kraków" to "małopolskie", "Wrocław" to "dolnośląskie",
    "Poznań" to "wielkopolskie", "Gdańsk" to "pomorskie", "Łódź" to "łódzkie",
    "Katowice" to "śląskie", "Szczecin" to "zachodniopomorskie", "Lublin" to "lubelskie",
    "Białystok" to "podlaskie", "Rzeszów" to "podkarpackie", "Bydgoszcz" to "kujawsko-pomorskie",
    "Opole" to "opolskie", "Kielce" to "świętokrzyskie", "Olsztyn" to "warmińsko-mazurskie",
    "Gorzów Wielkopolski" to "lubuskie",
)

val DEALER_NAMES = listOf(
    "AutoCentrum", "MotoPolska", "Auto Handel", "CarPoint", "Auto Salon", "Komis Samochodowy",
    "AutoPark", "Premium Cars", "Auto Bania", "MotoDealer", "Auto Serwis Kowalski",
)

val FIRST_NAMES = listOf(
    "Marek", "Tomasz", "Piotr", "Krzysztof", "Andrzej", "Paweł", "Michał", "Adam",
    "Anna", "Katarzyna", "Magdalena", "Agnieszka", "Ewa", "Joanna", "Marcin", "Grzegorz",
)
