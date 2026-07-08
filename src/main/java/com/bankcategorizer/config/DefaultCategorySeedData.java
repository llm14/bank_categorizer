package com.bankcategorizer.config;

import java.util.List;

/**
 * Static definition of the broad, generic set of default spending categories (and their
 * well-known merchant/brand keywords) seeded into a fresh, empty database by
 * {@link DefaultCategorySeeder}.
 *
 * <p>Kept as a plain, easy-to-extend data holder — add/remove {@link SeedCategory} entries
 * here to change what gets seeded on a fresh database; no other code needs to change.
 */
final class DefaultCategorySeedData {

    private DefaultCategorySeedData() {
    }

    /**
     * A single default category definition: name, short description, and the keywords that
     * should auto-match it.
     */
    record SeedCategory(String name, String description, List<String> keywords) {
    }

    /**
     * The default categories, in match-priority order (see
     * {@code CategorizationService#match}): categories listed earlier win when a description
     * could plausibly match more than one, e.g. "Dining & Takeaway" (UBER EATS) is listed
     * before "Transport & Fuel" (UBER) so food delivery isn't mistaken for a ride.
     */
    static final List<SeedCategory> DEFAULT_CATEGORIES = List.of(
            new SeedCategory("Groceries", "Supermarkets and grocery stores", List.of(
                    "MERCADONA", "CONTINENTE", "PINGO DOCE", "LIDL", "ALDI",
                    "CARREFOUR", "AUCHAN", "INTERMARCHÉ", "WALMART", "TESCO")),
            new SeedCategory("Dining & Takeaway", "Restaurants, fast food, and food delivery", List.of(
                    "MCDONALD'S", "BURGER KING", "KFC", "STARBUCKS",
                    "UBER EATS", "GLOVO", "DOMINO'S")),
            new SeedCategory("Transport & Fuel", "Ride-hailing, public transport, and fuel stations", List.of(
                    "UBER", "BOLT", "CABIFY", "GALP", "BP", "REPSOL", "CEPSA", "SHELL")),
            new SeedCategory("Subscriptions & Entertainment", "Streaming and other recurring subscriptions", List.of(
                    "NETFLIX", "SPOTIFY", "AMAZON PRIME", "DISNEY+", "HBO", "YOUTUBE PREMIUM")),
            new SeedCategory("Shopping", "General retail and online shopping", List.of(
                    "AMAZON", "EL CORTE INGLÉS", "IKEA", "ZARA", "H&M")),
            new SeedCategory("Utilities", "Electricity, gas, telecom, and other household utilities", List.of(
                    "EDP", "ENDESA", "VODAFONE", "MEO", "NOS", "IBERDROLA")),
            new SeedCategory("Travel", "Flights, accommodation, and travel bookings", List.of(
                    "BOOKING.COM", "AIRBNB", "RYANAIR", "TAP", "VUELING")),
            new SeedCategory("Health", "Pharmacy and medical consultation expenses", List.of(
                    "FARMACIA", "FARMÁCIA", "PHARMACY", "CLINICA", "CLÍNICA", "HOSPITAL", "CONSULTA MEDICA", "CONSULTA MÉDICA")),
            new SeedCategory("Credits", "Bank debt repayments such as mortgages and car loans", List.of(
                    "CREDITO HABITACAO", "CRÉDITO HABITAÇÃO", "CREDITO AUTOMOVEL", "CRÉDITO AUTOMÓVEL",
                    "HIPOTECA", "MORTGAGE", "EMPRESTIMO", "EMPRÉSTIMO", "PRESTACAO", "PRESTAÇÃO", "LOAN PAYMENT"))
    );
}
