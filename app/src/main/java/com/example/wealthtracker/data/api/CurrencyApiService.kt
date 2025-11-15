package com.example.wealthtracker.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class CurrencyInfo(
    val code: String,
    val name: String
)

data class CountryCurrencyData(
    val countryName: String,
    val countryCode: String,
    val flag: String,
    val currencyCode: String,
    val currencyName: String,
    val currencySymbol: String
)

object CurrencyApiService {
    private const val API_BASE_URL = "https://open.er-api.com/v6/latest/USD"
    
    // Comprehensive currency symbols mapping
    private val currencySymbols = mapOf(
        "USD" to "$", "EUR" to "€", "GBP" to "£", "INR" to "₹",
        "JPY" to "¥", "CNY" to "¥", "KRW" to "₩", "RUB" to "₽",
        "BRL" to "R$", "CAD" to "C$", "AUD" to "A$", "CHF" to "CHF",
        "MXN" to "Mex$", "SGD" to "S$", "HKD" to "HK$", "NZD" to "NZ$",
        "SEK" to "kr", "NOK" to "kr", "DKK" to "kr", "PLN" to "zł",
        "THB" to "฿", "IDR" to "Rp", "MYR" to "RM", "PHP" to "₱",
        "AED" to "د.إ", "SAR" to "﷼", "ZAR" to "R", "TRY" to "₺",
        "ILS" to "₪", "PKR" to "₨", "BDT" to "৳", "LKR" to "Rs",
        "NPR" to "₨", "VND" to "₫", "TWD" to "NT$", "CZK" to "Kč",
        "HUF" to "Ft", "RON" to "lei", "ARS" to "$", "CLP" to "$",
        "COP" to "$", "PEN" to "S/", "EGP" to "£", "NGN" to "₦",
        "KES" to "KSh", "GHS" to "₵"
    )
    
    // Map of country codes to country data
    private val countryData = mapOf(
        "US" to Triple("United States", "🇺🇸", "USD"),
        "GB" to Triple("United Kingdom", "🇬🇧", "GBP"),
        "IN" to Triple("India", "🇮🇳", "INR"),
        "CA" to Triple("Canada", "🇨🇦", "CAD"),
        "AU" to Triple("Australia", "🇦🇺", "AUD"),
        "SG" to Triple("Singapore", "🇸🇬", "SGD"),
        "AE" to Triple("United Arab Emirates", "🇦🇪", "AED"),
        "DE" to Triple("Germany", "🇩🇪", "EUR"),
        "FR" to Triple("France", "🇫🇷", "EUR"),
        "IT" to Triple("Italy", "🇮🇹", "EUR"),
        "ES" to Triple("Spain", "🇪🇸", "EUR"),
        "NL" to Triple("Netherlands", "🇳🇱", "EUR"),
        "CH" to Triple("Switzerland", "🇨🇭", "CHF"),
        "JP" to Triple("Japan", "🇯🇵", "JPY"),
        "CN" to Triple("China", "🇨🇳", "CNY"),
        "KR" to Triple("South Korea", "🇰🇷", "KRW"),
        "MY" to Triple("Malaysia", "🇲🇾", "MYR"),
        "TH" to Triple("Thailand", "🇹🇭", "THB"),
        "ID" to Triple("Indonesia", "🇮🇩", "IDR"),
        "PH" to Triple("Philippines", "🇵🇭", "PHP"),
        "VN" to Triple("Vietnam", "🇻🇳", "VND"),
        "BR" to Triple("Brazil", "🇧🇷", "BRL"),
        "MX" to Triple("Mexico", "🇲🇽", "MXN"),
        "ZA" to Triple("South Africa", "🇿🇦", "ZAR"),
        "NZ" to Triple("New Zealand", "🇳🇿", "NZD"),
        "SA" to Triple("Saudi Arabia", "🇸🇦", "SAR"),
        "TR" to Triple("Turkey", "🇹🇷", "TRY"),
        "RU" to Triple("Russia", "🇷🇺", "RUB"),
        "PK" to Triple("Pakistan", "🇵🇰", "PKR"),
        "BD" to Triple("Bangladesh", "🇧🇩", "BDT"),
        "LK" to Triple("Sri Lanka", "🇱🇰", "LKR"),
        "NP" to Triple("Nepal", "🇳🇵", "NPR"),
        "HK" to Triple("Hong Kong", "🇭🇰", "HKD"),
        "TW" to Triple("Taiwan", "🇹🇼", "TWD"),
        "IL" to Triple("Israel", "🇮🇱", "ILS"),
        "PL" to Triple("Poland", "🇵🇱", "PLN"),
        "SE" to Triple("Sweden", "🇸🇪", "SEK"),
        "NO" to Triple("Norway", "🇳🇴", "NOK"),
        "DK" to Triple("Denmark", "🇩🇰", "DKK"),
        "CZ" to Triple("Czech Republic", "🇨🇿", "CZK"),
        "RO" to Triple("Romania", "🇷🇴", "RON"),
        "AR" to Triple("Argentina", "🇦🇷", "ARS"),
        "CL" to Triple("Chile", "🇨🇱", "CLP"),
        "CO" to Triple("Colombia", "🇨🇴", "COP"),
        "PE" to Triple("Peru", "🇵🇪", "PEN"),
        "EG" to Triple("Egypt", "🇪🇬", "EGP"),
        "NG" to Triple("Nigeria", "🇳🇬", "NGN"),
        "KE" to Triple("Kenya", "🇰🇪", "KES"),
        "GH" to Triple("Ghana", "🇬🇭", "GHS"),
        "HU" to Triple("Hungary", "🇭🇺", "HUF"),
        "PT" to Triple("Portugal", "🇵🇹", "EUR"),
        "BE" to Triple("Belgium", "🇧🇪", "EUR"),
        "AT" to Triple("Austria", "🇦🇹", "EUR"),
        "IE" to Triple("Ireland", "🇮🇪", "EUR"),
        "FI" to Triple("Finland", "🇫🇮", "EUR"),
        "GR" to Triple("Greece", "🇬🇷", "EUR")
    )
    
    /**
     * Fetch supported currencies from ExchangeRate-API
     */
    suspend fun fetchSupportedCurrencies(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_BASE_URL)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(response)
            
            if (jsonObject.getString("result") == "success") {
                val rates = jsonObject.getJSONObject("rates")
                val currencies = mutableMapOf<String, String>()
                
                rates.keys().forEach { currencyCode ->
                    currencies[currencyCode] = getCurrencyName(currencyCode)
                }
                
                Result.success(currencies)
            } else {
                Result.failure(Exception("API returned error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get mapped countries with their currency data
     */
    fun getMappedCountries(supportedCurrencies: Map<String, String>): List<CountryCurrencyData> {
        return countryData.mapNotNull { (countryCode, data) ->
            val (countryName, flag, currencyCode) = data
            if (supportedCurrencies.containsKey(currencyCode)) {
                CountryCurrencyData(
                    countryName = countryName,
                    countryCode = countryCode,
                    flag = flag,
                    currencyCode = currencyCode,
                    currencyName = supportedCurrencies[currencyCode] ?: currencyCode,
                    currencySymbol = currencySymbols[currencyCode] ?: currencyCode
                )
            } else null
        }.sortedBy { it.countryName }
    }
    
    /**
     * Get default fallback countries (for offline mode)
     */
    fun getDefaultCountries(): List<CountryCurrencyData> {
        return countryData.map { (countryCode, data) ->
            val (countryName, flag, currencyCode) = data
            CountryCurrencyData(
                countryName = countryName,
                countryCode = countryCode,
                flag = flag,
                currencyCode = currencyCode,
                currencyName = getCurrencyName(currencyCode),
                currencySymbol = currencySymbols[currencyCode] ?: currencyCode
            )
        }.sortedBy { it.countryName }
    }
    
    fun getCurrencySymbol(currencyCode: String): String {
        return currencySymbols[currencyCode] ?: currencyCode
    }
    
    private fun getCurrencyName(code: String): String {
        return when (code) {
            "USD" -> "US Dollar"
            "EUR" -> "Euro"
            "GBP" -> "British Pound"
            "INR" -> "Indian Rupee"
            "JPY" -> "Japanese Yen"
            "CNY" -> "Chinese Yuan"
            "KRW" -> "South Korean Won"
            "RUB" -> "Russian Ruble"
            "BRL" -> "Brazilian Real"
            "CAD" -> "Canadian Dollar"
            "AUD" -> "Australian Dollar"
            "CHF" -> "Swiss Franc"
            "MXN" -> "Mexican Peso"
            "SGD" -> "Singapore Dollar"
            "HKD" -> "Hong Kong Dollar"
            "NZD" -> "New Zealand Dollar"
            "SEK" -> "Swedish Krona"
            "NOK" -> "Norwegian Krone"
            "DKK" -> "Danish Krone"
            "PLN" -> "Polish Zloty"
            "THB" -> "Thai Baht"
            "IDR" -> "Indonesian Rupiah"
            "MYR" -> "Malaysian Ringgit"
            "PHP" -> "Philippine Peso"
            "AED" -> "UAE Dirham"
            "SAR" -> "Saudi Riyal"
            "ZAR" -> "South African Rand"
            "TRY" -> "Turkish Lira"
            "ILS" -> "Israeli Shekel"
            "PKR" -> "Pakistani Rupee"
            "BDT" -> "Bangladeshi Taka"
            "LKR" -> "Sri Lankan Rupee"
            "NPR" -> "Nepalese Rupee"
            "VND" -> "Vietnamese Dong"
            "TWD" -> "Taiwan Dollar"
            "CZK" -> "Czech Koruna"
            "HUF" -> "Hungarian Forint"
            "RON" -> "Romanian Leu"
            "ARS" -> "Argentine Peso"
            "CLP" -> "Chilean Peso"
            "COP" -> "Colombian Peso"
            "PEN" -> "Peruvian Sol"
            "EGP" -> "Egyptian Pound"
            "NGN" -> "Nigerian Naira"
            "KES" -> "Kenyan Shilling"
            "GHS" -> "Ghanaian Cedi"
            else -> code
        }
    }
}
