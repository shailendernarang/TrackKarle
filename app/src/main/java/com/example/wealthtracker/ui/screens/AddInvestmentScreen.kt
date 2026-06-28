package com.example.wealthtracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.ui.InvestmentViewModel
import com.example.wealthtracker.util.FormatUtils

// ─── Page state ────────────────────────────────────────────────────────────────
private enum class AddInvPage { HOME, CATEGORIES, CATEGORY_DETAIL }

private val FgBlue = Color(0xFF155DFC)

// ─── Data models ───────────────────────────────────────────────────────────────
private data class InvCategory(
    val name: String,
    val emoji: String,
    val iconBgColor: Color,
    val types: List<InvTypeItem>
)

private data class InvTypeItem(
    val display: String,
    val subtitle: String,
    val investmentType: String,
    val preOthersName: String? = null,
    val emoji: String = "",
    val iconBgColor: Color = Color(0xFFEFF6FF)
)

// ─── Category definitions (exact Figma colors) ─────────────────────────────────
private val investmentCategories = listOf(
    InvCategory("Banking", "🏦", Color(0xFFEFF6FF), listOf(
        InvTypeItem("Fixed Deposit", "Guaranteed returns with a bank", "FD", null, "🏦", Color(0xFFEFF6FF)),
        InvTypeItem("Savings Account", "Regular bank savings account", "Others", "Savings Account", "🏦", Color(0xFFEFF6FF)),
        InvTypeItem("Recurring Deposit", "Monthly fixed deposits", "Others", "Recurring Deposit", "🏦", Color(0xFFEFF6FF))
    )),
    InvCategory("Mutual Funds", "📊", Color(0xFFECFDF5), listOf(
        InvTypeItem("Mutual Fund", "Professionally managed fund", "Mutual Fund", null, "📊", Color(0xFFECFDF5)),
        InvTypeItem("SIP", "Systematic Investment Plan", "Mutual Fund", null, "📊", Color(0xFFECFDF5))
    )),
    InvCategory("Stocks & ETFs", "📈", Color(0xFFFFFBEB), listOf(
        InvTypeItem("Stocks", "Individual company shares", "Equity", null, "📈", Color(0xFFFFFBEB)),
        InvTypeItem("ETFs", "Exchange-traded funds", "Equity", "ETF", "📈", Color(0xFFFFFBEB))
    )),
    InvCategory("Bonds", "🔗", Color(0xFFF5F3FF), listOf(
        InvTypeItem("Bonds", "Fixed income securities", "Others", "Bonds", "🔗", Color(0xFFF5F3FF)),
        InvTypeItem("Corporate Bonds", "Company issued bonds", "Others", "Corporate Bonds", "🔗", Color(0xFFF5F3FF))
    )),
    InvCategory("Retirement", "🏖️", Color(0xFFECFEFF), listOf(
        InvTypeItem("NPS", "National Pension System", "NPS", null, "🏛️", Color(0xFFEFF6FF)),
        InvTypeItem("PPF", "Public Provident Fund", "PPF", null, "🏛️", Color(0xFFEFF6FF)),
        InvTypeItem("EPF", "Employee Provident Fund", "EPF", null, "🏢", Color(0xFFEFF6FF))
    )),
    InvCategory("Insurance", "🛡️", Color(0xFFFEF2F2), listOf(
        InvTypeItem("Term Insurance", "Life cover at low premium", "Term Insurance", null, "🛡️", Color(0xFFFEF2F2)),
        InvTypeItem("Health Insurance", "Medical coverage policy", "Health Insurance", null, "🛡️", Color(0xFFFEF2F2))
    )),
    InvCategory("Gold & Commodities", "🥇", Color(0xFFFEF3C7), listOf(
        InvTypeItem("Gold", "Physical or digital gold", "Gold", null, "🥇", Color(0xFFFEF3C7)),
        InvTypeItem("Silver", "Physical or digital silver", "Gold", "Silver", "🥈", Color(0xFFFEF3C7))
    )),
    InvCategory("Crypto", "₿", Color(0xFFFFF7ED), listOf(
        InvTypeItem("Crypto", "Digital currencies & tokens", "Others", "Crypto", "₿", Color(0xFFFFF7ED))
    )),
    InvCategory("Real Estate", "🏠", Color(0xFFF0FDF4), listOf(
        InvTypeItem("Real Estate", "Property investment", "Others", "Real Estate", "🏠", Color(0xFFF0FDF4)),
        InvTypeItem("REITs", "Real estate investment trust", "Others", "REITs", "🏢", Color(0xFFF0FDF4))
    )),
    InvCategory("Govt Schemes", "🏛️", Color(0xFFEFF6FF), listOf(
        InvTypeItem("PPF", "Public Provident Fund", "PPF", null, "🏛️", Color(0xFFEFF6FF)),
        InvTypeItem("NPS", "National Pension System", "NPS", null, "🏛️", Color(0xFFEFF6FF)),
        InvTypeItem("Sukanya Samriddhi", "Girl child savings scheme", "Others", "Sukanya Samriddhi", "💛", Color(0xFFEFF6FF)),
        InvTypeItem("SCSS", "Senior Citizens Savings Scheme", "Others", "SCSS", "👴", Color(0xFFEFF6FF)),
        InvTypeItem("KVP", "Kisan Vikas Patra — doubles investment", "Others", "KVP", "🌾", Color(0xFFEFF6FF)),
        InvTypeItem("NSC", "National Savings Certificate", "Others", "NSC", "📜", Color(0xFFEFF6FF)),
        InvTypeItem("MIS", "Post Office Monthly Income Scheme", "Others", "MIS", "📮", Color(0xFFEFF6FF)),
        InvTypeItem("MSSC", "Mahila Samman Savings Certificate", "Others", "MSSC", "👩", Color(0xFFEFF6FF))
    )),
    InvCategory("Americas 🌎", "🌎", Color(0xFFEFF6FF), listOf(
        InvTypeItem("401(k)", "US employer-sponsored retirement plan", "RetirementAccount", "401k", "🏛️", Color(0xFFECFEFF)),
        InvTypeItem("IRA", "US Individual Retirement Account", "RetirementAccount", "IRA", "🏛️", Color(0xFFECFEFF)),
        InvTypeItem("Roth IRA", "US tax-free growth retirement account", "RetirementAccount", "Roth IRA", "🏛️", Color(0xFFECFEFF)),
        InvTypeItem("HSA", "US Health Savings Account", "Others", "HSA", "🏥", Color(0xFFFEF2F2)),
        InvTypeItem("529 Plan", "US college education savings plan", "Others", "529 Plan", "🎓", Color(0xFFECFDF5)),
        InvTypeItem("TSP", "US Thrift Savings Plan (federal employees)", "RetirementAccount", "TSP", "🏛️", Color(0xFFECFEFF)),
        InvTypeItem("US Treasuries", "US government bonds, bills & TIPS", "GovernmentBond", "US Treasuries", "📜", Color(0xFFEFF6FF)),
        InvTypeItem("I Bonds", "US inflation-linked savings bonds", "GovernmentBond", "I Bonds", "📜", Color(0xFFEFF6FF)),
        InvTypeItem("RRSP", "Canadian registered retirement savings plan", "RetirementAccount", "RRSP", "🍁", Color(0xFFECFEFF)),
        InvTypeItem("TFSA", "Canadian tax-free savings account", "Others", "TFSA", "🍁", Color(0xFFECFDF5)),
        InvTypeItem("RESP", "Canadian registered education savings plan", "Others", "RESP", "🎓", Color(0xFFECFDF5)),
        InvTypeItem("FHSA", "Canadian first home savings account", "Others", "FHSA", "🏠", Color(0xFFF0FDF4))
    )),
    InvCategory("Europe 🌍", "🌍", Color(0xFFF5F3FF), listOf(
        InvTypeItem("ISA", "UK Individual Savings Account", "Others", "ISA", "🇬🇧", Color(0xFFEFF6FF)),
        InvTypeItem("Lifetime ISA", "UK savings for first home or retirement", "Others", "Lifetime ISA", "🏠", Color(0xFFEFF6FF)),
        InvTypeItem("SIPP", "UK self-invested personal pension", "RetirementAccount", "SIPP", "🏛️", Color(0xFFECFEFF)),
        InvTypeItem("Premium Bonds", "UK NS&I prize savings bonds", "GovernmentBond", "Premium Bonds", "🎰", Color(0xFFEFF6FF)),
        InvTypeItem("UK Gilts", "UK government bonds", "GovernmentBond", "UK Gilts", "📜", Color(0xFFEFF6FF)),
        InvTypeItem("PEA", "French equity savings plan (Plan d'Épargne en Actions)", "Others", "PEA", "🇫🇷", Color(0xFFFFFBEB)),
        InvTypeItem("Livret A", "French regulated savings account", "Others", "Livret A", "🇫🇷", Color(0xFFECFDF5)),
        InvTypeItem("Assurance-vie", "French life insurance savings wrapper", "Others", "Assurance-vie", "🇫🇷", Color(0xFFFEF2F2)),
        InvTypeItem("Riester-Rente", "German state-subsidised pension plan", "RetirementAccount", "Riester-Rente", "🇩🇪", Color(0xFFECFEFF))
    )),
    InvCategory("Asia Pacific 🌏", "🌏", Color(0xFFECFDF5), listOf(
        InvTypeItem("CPF-OA", "Singapore CPF Ordinary Account", "RetirementAccount", "CPF-OA", "🇸🇬", Color(0xFFECFEFF)),
        InvTypeItem("CPF-SA", "Singapore CPF Special Account", "RetirementAccount", "CPF-SA", "🇸🇬", Color(0xFFECFEFF)),
        InvTypeItem("SRS", "Singapore Supplementary Retirement Scheme", "Others", "SRS", "🇸🇬", Color(0xFFECFEFF)),
        InvTypeItem("SSB", "Singapore Savings Bonds", "GovernmentBond", "Singapore Savings Bonds", "📜", Color(0xFFEFF6FF)),
        InvTypeItem("NISA", "Japanese tax-exempt investment account", "Others", "NISA", "🇯🇵", Color(0xFFFFFBEB)),
        InvTypeItem("iDeCo", "Japanese individual defined-contribution pension", "RetirementAccount", "iDeCo", "🇯🇵", Color(0xFFECFEFF)),
        InvTypeItem("JGBs", "Japanese government bonds", "GovernmentBond", "JGBs", "📜", Color(0xFFEFF6FF)),
        InvTypeItem("Superannuation", "Australian mandatory retirement fund", "RetirementAccount", "Superannuation", "🇦🇺", Color(0xFFECFEFF)),
        InvTypeItem("FHSSS", "Australian first home super saver scheme", "Others", "FHSSS", "🏠", Color(0xFFF0FDF4)),
        InvTypeItem("KiwiSaver", "New Zealand retirement savings scheme", "RetirementAccount", "KiwiSaver", "🇳🇿", Color(0xFFECFEFF)),
        InvTypeItem("Kiwi Bonds", "New Zealand government bonds", "GovernmentBond", "Kiwi Bonds", "📜", Color(0xFFEFF6FF))
    )),
    InvCategory("Alternative", "💡", Color(0xFFF5F3FF), listOf(
        InvTypeItem("P2P Lending", "Peer-to-peer lending", "Others", "P2P Lending", "🤝", Color(0xFFF5F3FF)),
        InvTypeItem("Angel Investment", "Early stage startup", "Others", "Angel Investment", "👼", Color(0xFFF5F3FF)),
        InvTypeItem("Others", "Any other investment type", "Others", null, "💡", Color(0xFFF5F3FF))
    )),
    InvCategory("Cash & Deposits", "💵", Color(0xFFF0FDF4), listOf(
        InvTypeItem("Fixed Deposit", "Guaranteed returns with a bank", "FD", null, "🏦", Color(0xFFEFF6FF)),
        InvTypeItem("Liquid Fund", "High liquidity cash fund", "Mutual Fund", "Liquid Fund", "💧", Color(0xFFECFDF5)),
        InvTypeItem("Savings Account", "Regular bank savings", "Others", "Savings Account", "💵", Color(0xFFF0FDF4))
    ))
)

private val popularInvestments = listOf(
    InvTypeItem("Fixed Deposit", "Guaranteed returns with a bank", "FD", null, "🏦", Color(0xFFEFF6FF)),
    InvTypeItem("SIP / Mutual Fund", "Systematic investment plan", "Mutual Fund", null, "📊", Color(0xFFECFDF5)),
    InvTypeItem("Stocks", "Individual company shares", "Equity", null, "📈", Color(0xFFFFFBEB)),
    InvTypeItem("Gold", "Physical or digital gold", "Gold", null, "🥇", Color(0xFFFEF3C7)),
    InvTypeItem("NPS", "National pension system", "NPS", null, "🏛️", Color(0xFFEFF6FF)),
    InvTypeItem("PPF", "Public provident fund", "PPF", null, "🏛️", Color(0xFFEFF6FF))
)

private fun iconBgFor(type: String): Color = when (type) {
    "FD" -> Color(0xFFEFF6FF)
    "Mutual Fund" -> Color(0xFFECFDF5)
    "Equity", "Stocks" -> Color(0xFFFFFBEB)
    "Gold" -> Color(0xFFFEF3C7)
    "NPS", "PPF", "EPF" -> Color(0xFFEFF6FF)
    "Term Insurance", "Health Insurance" -> Color(0xFFFEF2F2)
    "GovernmentBond" -> Color(0xFFEFF6FF)
    "RetirementAccount" -> Color(0xFFECFEFF)
    else -> Color(0xFFF5F3FF)
}

private fun emojiFor(type: String): String = when (type) {
    "FD" -> "🏦"
    "Mutual Fund" -> "📊"
    "Equity", "Stocks" -> "📈"
    "Gold" -> "🥇"
    "NPS", "PPF" -> "🏛️"
    "EPF" -> "🏢"
    "Term Insurance", "Health Insurance" -> "🛡️"
    "GovernmentBond" -> "📜"
    "RetirementAccount" -> "🏖️"
    else -> "💼"
}

private fun categoryFor(type: String): String = when (type) {
    "FD" -> "Banking"
    "Mutual Fund" -> "Mutual Funds"
    "Equity", "Stocks" -> "Stocks & ETFs"
    "Gold" -> "Gold & Commodities"
    "NPS", "PPF", "EPF" -> "Retirement"
    "Term Insurance", "Health Insurance" -> "Insurance"
    "GovernmentBond" -> "Bonds"
    "RetirementAccount" -> "Retirement"
    else -> "Others"
}

// ─── Root composable ───────────────────────────────────────────────────────────
@Composable
fun AddInvestmentScreen(
    viewModel: InvestmentViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onTypeSelected: (investmentType: String, preOthersName: String?) -> Unit
) {
    val allInvestments by viewModel.investments.collectAsState()
    val totalAmount = allInvestments.sumOf { it.amount }
    val recentlyAdded = remember(allInvestments) {
        allInvestments.sortedByDescending { it.createdAt }.distinctBy { it.investmentType }.take(5)
    }

    var page by remember { mutableStateOf(AddInvPage.HOME) }
    var selectedCategory by remember { mutableStateOf<InvCategory?>(null) }
    var categorySource by remember { mutableStateOf(AddInvPage.HOME) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler {
        when (page) {
            AddInvPage.HOME -> onBack()
            AddInvPage.CATEGORIES -> { page = AddInvPage.HOME; searchQuery = "" }
            AddInvPage.CATEGORY_DETAIL -> page = categorySource
        }
    }

    when (page) {
        AddInvPage.HOME -> AddInvHomePage(
            totalAmount = totalAmount, investmentCount = allInvestments.size,
            recentlyAdded = recentlyAdded, searchQuery = searchQuery,
            onSearchChange = { searchQuery = it }, onBack = onBack,
            onSeeAllCategories = { page = AddInvPage.CATEGORIES; searchQuery = "" },
            onCategoryClick = { cat -> selectedCategory = cat; categorySource = AddInvPage.HOME; page = AddInvPage.CATEGORY_DETAIL },
            onTypeSelected = onTypeSelected
        )
        AddInvPage.CATEGORIES -> AddInvCategoriesPage(
            searchQuery = searchQuery, onSearchChange = { searchQuery = it },
            onBack = { page = AddInvPage.HOME; searchQuery = "" },
            onCategoryClick = { cat -> selectedCategory = cat; categorySource = AddInvPage.CATEGORIES; page = AddInvPage.CATEGORY_DETAIL }
        )
        AddInvPage.CATEGORY_DETAIL -> AddInvCategoryDetailPage(
            category = selectedCategory!!, onBack = { page = categorySource },
            onTypeSelected = onTypeSelected
        )
    }
}

// ─── Page 1: Home ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddInvHomePage(
    totalAmount: Double, investmentCount: Int, recentlyAdded: List<InvestmentEntity>,
    searchQuery: String, onSearchChange: (String) -> Unit, onBack: () -> Unit,
    onSeeAllCategories: () -> Unit, onCategoryClick: (InvCategory) -> Unit,
    onTypeSelected: (String, String?) -> Unit
) {
    val isSearching = searchQuery.isNotBlank()
    val searchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else investmentCategories.flatMap { cat ->
            cat.types.filter {
                it.display.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true) ||
                it.investmentType.contains(searchQuery, ignoreCase = true) ||
                (it.preOthersName?.contains(searchQuery, ignoreCase = true) == true) ||
                cat.name.contains(searchQuery, ignoreCase = true)
            }.map { it to cat.name }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                title = {
                    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
                    val greeting = when {
                        hour < 12 -> "Good morning 👋"
                        hour < 17 -> "Good afternoon 👋"
                        else -> "Good evening 👋"
                    }
                    Column {
                        Text(greeting, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal, letterSpacing = (-0.16).sp)
                        Text("Add Investment", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                FigmaSearchBar(searchQuery, onSearchChange,
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
            }

            if (isSearching) {
                if (searchResults.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            Text("No results for \"$searchQuery\"", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    item {
                        Text("${searchResults.size} result${if (searchResults.size != 1) "s" else ""}",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    items(searchResults) { (typeItem, catName) ->
                        TypeListItem(typeItem, subtitle = catName,
                            onClick = { onTypeSelected(typeItem.investmentType, typeItem.preOthersName) })
                    }
                }
            } else {
                item {
                    SectionHeader("What do you want to add?", "See all", onSeeAllCategories,
                        Modifier.padding(horizontal = 16.dp).padding(top = 4.dp))
                }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(investmentCategories) { cat ->
                            CategoryChip(cat) { onCategoryClick(cat) }
                        }
                    }
                }
                if (recentlyAdded.isNotEmpty()) {
                    item {
                        SectionHeader("Recently Added", modifier =
                            Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(recentlyAdded) { entity ->
                                RecentlyAddedChip(entity, onClick = {
                                    val normalizedType = if (entity.investmentType == "Stocks") "Equity" else entity.investmentType
                                    val preOthers = if (normalizedType == "Others") entity.type.takeIf { it.isNotBlank() } else null
                                    onTypeSelected(normalizedType, preOthers)
                                })
                            }
                        }
                    }
                }
                item {
                    SectionHeader("Popular Investments", "See all", onSeeAllCategories,
                        Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 4.dp))
                }
                items(popularInvestments) { item ->
                    TypeListItem(item, onClick = { onTypeSelected(item.investmentType, item.preOthersName) })
                }
                item {
                    com.example.wealthtracker.ui.components.AppodealNativeNewsFeed(
                        modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

// ─── Page 2: Browse Categories ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddInvCategoriesPage(
    searchQuery: String, onSearchChange: (String) -> Unit,
    onBack: () -> Unit, onCategoryClick: (InvCategory) -> Unit
) {
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) investmentCategories
        else investmentCategories.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                title = {
                    Text("Browse Categories", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(MaterialTheme.colorScheme.surface)) {
            FigmaSearchBar(searchQuery, onSearchChange,
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { cat -> CategoryGridCard(cat) { onCategoryClick(cat) } }
            }
        }
    }
}

// ─── Page 3: Category Detail ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddInvCategoryDetailPage(
    category: InvCategory, onBack: () -> Unit,
    onTypeSelected: (String, String?) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                title = {
                    Text(category.name, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(Modifier.padding(17.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                            .background(category.iconBgColor), Alignment.Center) {
                            Text(category.emoji, fontSize = 24.sp)
                        }
                        Column {
                            Text(category.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp)
                            Text("${category.types.size} types available", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = (-0.16).sp)
                        }
                    }
                }
            }
            items(category.types) { typeItem ->
                TypeListItem(typeItem, showArrow = true,
                    onClick = { onTypeSelected(typeItem.investmentType, typeItem.preOthersName) })
            }
        }
    }
}

// ─── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun WalletCard(totalAmount: Double, investmentCount: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED))))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your Investments", fontSize = 12.sp, color = Color(0xFFDBEAFE),
                fontWeight = FontWeight.Normal, letterSpacing = (-0.16).sp)
            Text(FormatUtils.formatINR(totalAmount), fontSize = 30.sp, color = Color.White,
                fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (investmentCount == 0) "0 assets" else "$investmentCount assets",
                        fontSize = 12.sp, color = Color(0xFFB9F8CF),
                        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.16).sp
                    )
                }
                Text("tracked", fontSize = 12.sp, color = Color(0xFFBEDBFF),
                    fontWeight = FontWeight.Normal, letterSpacing = (-0.16).sp)
            }
        }
    }
}

@Composable
private fun FigmaSearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.16).sp),
            decorationBox = { inner ->
                Row(
                    Modifier.padding(horizontal = 17.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("Search investments...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.16).sp)
                        }
                        inner()
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String, actionLabel: String? = null,
    onAction: (() -> Unit)? = null, modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp)
        if (actionLabel != null && onAction != null) {
            Text(actionLabel, fontSize = 12.sp, color = FgBlue,
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.16).sp,
                modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
private fun CategoryChip(category: InvCategory, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(90.dp).wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                .background(category.iconBgColor), Alignment.Center) {
                Text(category.emoji, fontSize = 20.sp, textAlign = TextAlign.Center)
            }
            Text(category.name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.16).sp,
                textAlign = TextAlign.Center, maxLines = 2, softWrap = true,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecentlyAddedChip(entity: InvestmentEntity, onClick: () -> Unit) {
    val displayName = when (entity.investmentType) {
        "FD" -> "Fixed Deposit"
        "Mutual Fund" -> "Mutual Fund"
        "Equity", "Stocks" -> "Stocks"
        else -> entity.type.ifBlank { entity.investmentType }
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(20.dp))
                .background(iconBgFor(entity.investmentType)), Alignment.Center) {
                Text(emojiFor(entity.investmentType), fontSize = 16.sp)
            }
            Column {
                Text(displayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp, maxLines = 1)
                Text(categoryFor(entity.investmentType), fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.16).sp)
            }
        }
    }
}

@Composable
private fun CategoryGridCard(category: InvCategory, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(87.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                .background(category.iconBgColor), Alignment.Center) {
                Text(category.emoji, fontSize = 24.sp)
            }
            Column {
                Text(category.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp)
                Text("${category.types.size} types", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-0.16).sp)
            }
        }
    }
}

@Composable
private fun TypeListItem(
    item: InvTypeItem, onClick: () -> Unit,
    subtitle: String? = null, showArrow: Boolean = true
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(20.dp))
            .background(item.iconBgColor), Alignment.Center) {
            Text(item.emoji.ifBlank { "💼" }, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(item.display, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp)
            Text(subtitle ?: item.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal, letterSpacing = (-0.16).sp)
        }
        if (showArrow) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), 0.5.dp, MaterialTheme.colorScheme.outlineVariant)
}
