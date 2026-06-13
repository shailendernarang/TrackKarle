package com.example.wealthtracker.util

data class CountryInfo(
    val name: String,
    val code: String,
    val flag: String,
    val currency: String,
    val currencySymbol: String
)

object CountryCurrency {
    val countries = listOf(
        // Major markets
        CountryInfo("India", "IN", "🇮🇳", "INR", "₹"),
        CountryInfo("United States", "US", "🇺🇸", "USD", "$"),
        CountryInfo("United Kingdom", "GB", "🇬🇧", "GBP", "£"),
        CountryInfo("Canada", "CA", "🇨🇦", "CAD", "C$"),
        CountryInfo("Australia", "AU", "🇦🇺", "AUD", "A$"),
        CountryInfo("Singapore", "SG", "🇸🇬", "SGD", "S$"),
        CountryInfo("United Arab Emirates", "AE", "🇦🇪", "AED", "د.إ"),
        
        // European countries
        CountryInfo("Germany", "DE", "🇩🇪", "EUR", "€"),
        CountryInfo("France", "FR", "🇫🇷", "EUR", "€"),
        CountryInfo("Italy", "IT", "🇮🇹", "EUR", "€"),
        CountryInfo("Spain", "ES", "🇪🇸", "EUR", "€"),
        CountryInfo("Netherlands", "NL", "🇳🇱", "EUR", "€"),
        CountryInfo("Switzerland", "CH", "🇨🇭", "CHF", "CHF"),
        
        // Asian countries
        CountryInfo("Japan", "JP", "🇯🇵", "JPY", "¥"),
        CountryInfo("China", "CN", "🇨🇳", "CNY", "¥"),
        CountryInfo("South Korea", "KR", "🇰🇷", "KRW", "₩"),
        CountryInfo("Malaysia", "MY", "🇲🇾", "MYR", "RM"),
        CountryInfo("Thailand", "TH", "🇹🇭", "THB", "฿"),
        CountryInfo("Indonesia", "ID", "🇮🇩", "IDR", "Rp"),
        CountryInfo("Philippines", "PH", "🇵🇭", "PHP", "₱"),
        CountryInfo("Vietnam", "VN", "🇻🇳", "VND", "₫"),
        
        // Other major markets
        CountryInfo("Brazil", "BR", "🇧🇷", "BRL", "R$"),
        CountryInfo("Mexico", "MX", "🇲🇽", "MXN", "Mex$"),
        CountryInfo("South Africa", "ZA", "🇿🇦", "ZAR", "R"),
        CountryInfo("New Zealand", "NZ", "🇳🇿", "NZD", "NZ$"),
        CountryInfo("Saudi Arabia", "SA", "🇸🇦", "SAR", "﷼"),
        CountryInfo("Turkey", "TR", "🇹🇷", "TRY", "₺"),
        CountryInfo("Russia", "RU", "🇷🇺", "RUB", "₽"),
        
        // More countries
        CountryInfo("Pakistan", "PK", "🇵🇰", "PKR", "₨"),
        CountryInfo("Bangladesh", "BD", "🇧🇩", "BDT", "৳"),
        CountryInfo("Sri Lanka", "LK", "🇱🇰", "LKR", "Rs"),
        CountryInfo("Nepal", "NP", "🇳🇵", "NPR", "₨"),
        CountryInfo("Hong Kong", "HK", "🇭🇰", "HKD", "HK$"),
        CountryInfo("Taiwan", "TW", "🇹🇼", "TWD", "NT$"),
        CountryInfo("Israel", "IL", "🇮🇱", "ILS", "₪"),
        CountryInfo("Poland", "PL", "🇵🇱", "PLN", "zł"),
        CountryInfo("Sweden", "SE", "🇸🇪", "SEK", "kr"),
        CountryInfo("Norway", "NO", "🇳🇴", "NOK", "kr"),
        CountryInfo("Denmark", "DK", "🇩🇰", "DKK", "kr"),
        CountryInfo("Czech Republic", "CZ", "🇨🇿", "CZK", "Kč"),
        CountryInfo("Romania", "RO", "🇷🇴", "RON", "lei"),
        CountryInfo("Argentina", "AR", "🇦🇷", "ARS", "$"),
        CountryInfo("Chile", "CL", "🇨🇱", "CLP", "$"),
        CountryInfo("Colombia", "CO", "🇨🇴", "COP", "$"),
        CountryInfo("Peru", "PE", "🇵🇪", "PEN", "S/"),
        CountryInfo("Egypt", "EG", "🇪🇬", "EGP", "£"),
        CountryInfo("Nigeria", "NG", "🇳🇬", "NGN", "₦"),
        CountryInfo("Kenya", "KE", "🇰🇪", "KES", "KSh"),
        CountryInfo("Ghana", "GH", "🇬🇭", "GHS", "₵")
    )
    
    fun getCountryByCode(code: String): CountryInfo? {
        return countries.find { it.code == code }
    }
    
    fun getCurrencySymbol(countryCode: String): String {
        return getCountryByCode(countryCode)?.currencySymbol ?: "$"
    }
    
    fun getCurrencyCode(countryCode: String): String {
        return getCountryByCode(countryCode)?.currency ?: "USD"
    }
    
    // Default to India if no country is set
    fun getDefaultCountry(): CountryInfo {
        return countries.first { it.code == "IN" }
    }
}
