package com.example.envo.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.envo.data.model.TemperatureHistory
import com.example.envo.ui.theme.*
import com.example.envo.ui.viewmodel.EcoScopeViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.rememberRipple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Switch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.envo.ui.theme.rememberDarkModeState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun EcoScopeScreen(
    viewModel: EcoScopeViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var rssItems by remember { mutableStateOf<List<RssItem>>(emptyList()) }
    var rssLoading by remember { mutableStateOf(false) }
    var rssError by remember { mutableStateOf<String?>(null) }
    val rssUrl = "https://www.nasa.gov/rss/dyn/breaking_news.rss"
    val systemDark = isSystemInDarkTheme()
    var isDark by remember { mutableStateOf(systemDark) }
    val coroutineScope = rememberCoroutineScope()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewModel.environmentalData.value.isLoading,
        onRefresh = {
            coroutineScope.launch {
                viewModel.fetchEnvironmentalData(viewModel.currentLocation.value.latitude, viewModel.currentLocation.value.longitude)
            }
        }
    )
    // Environmental tips
    val environmentalTips = listOf(
        "Use public transport or carpool to reduce carbon emissions",
        "Switch to energy-efficient LED bulbs to save electricity",
        "Reduce, reuse, and recycle to minimize waste",
        "Plant trees and maintain green spaces in your community",
        "Use reusable water bottles and shopping bags",
        "Support local and sustainable food production",
        "Turn off lights and unplug devices when not in use",
        "Choose eco-friendly cleaning products",
        "Conserve water by fixing leaks and using water-saving fixtures",
        "Educate others about environmental conservation"
    )
    // FIX: Move horizontalScrollState here
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        try {
            // Initial setup
        } catch (e: Exception) {
            Log.e("EcoScopeScreen", "Error during screen initialization", e)
            Toast.makeText(context, "Failed to initialize app", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(rssUrl) {
        rssLoading = true
        rssError = null
        try {
            rssItems = fetchRss(rssUrl)
        } catch (e: Exception) {
            rssError = "Failed to load news: ${e.localizedMessage}"
        } finally {
            rssLoading = false
        }
    }

    MaterialTheme(
        colorScheme = if (isDark) nightColorScheme else LightColorScheme
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (isDark) NightSkyHeader()
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar (Fixed)
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = "Eco Icon",
                                tint = LeafGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EcoScope",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    },
                    actions = {
                        // Dark mode toggle in top bar
                        IconButton(onClick = { isDark = !isDark }) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle dark mode",
                                tint = White
                            )
                        }
                        GlowingLeafIcon(temperature = viewModel.environmentalData.value.temperature)
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkBlue,
                        titleContentColor = White,
                        actionIconContentColor = White
                    )
                )
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 16.dp)
                        .pullRefresh(pullRefreshState)
                ) {
                    // Search Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = viewModel.locationName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search for a location", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Icon",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = LeafGreen,
                                cursorColor = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotEmpty()) {
                                        try {
                                            viewModel.searchLocation(searchQuery, context)
                                        } catch (e: Exception) {
                                            Log.e("EcoScopeScreen", "Error during search", e)
                                            Toast.makeText(context, "Search failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Please enter a location", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ),
                            singleLine = true,
                            enabled = !viewModel.environmentalData.value.isLoading
                        )
                    }

                    // Current Data Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Current Data",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (viewModel.environmentalData.value.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            } else {
                                if (viewModel.environmentalData.value.isEstimated) {
                                    Text(
                                        text = "⚠️ Using estimated data",
                                        color = Color(0xFFFFA000),
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .padding(bottom = 8.dp)
                                    )
                                }
                                
                                // Temperature
                                DataRow(
                                    icon = Icons.Default.Thermostat,
                                    title = "Air Temperature (2m above ground)",
                                    value = String.format("%.1f°C", viewModel.environmentalData.value.temperature),
                                    color = TemperatureRed
                                )
                                
                                // Cloud Cover
                                DataRow(
                                    icon = Icons.Default.Cloud,
                                    title = "Cloud Cover",
                                    value = String.format("%.0f%%", viewModel.environmentalData.value.cloudCover),
                                    color = Color(0xFF64B5F6)
                                )
                                
                                // Solar Radiation
                                DataRow(
                                    icon = Icons.Default.WbSunny,
                                    title = "Solar Radiation",
                                    value = String.format("%.1f W/m²", viewModel.environmentalData.value.solarRadiation),
                                    color = AerosolOrange
                                )
                            }
                        }
                    }

                    // Environmental Tips Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Eco-Friendly Living Tips",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Horizontal scrolling tips
                        Row(
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState)
                                .fillMaxWidth()
                        ) {
                            environmentalTips.forEach { tip ->
                                Card(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .padding(end = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Eco,
                                            contentDescription = "Eco Tip",
                                            tint = LeafGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = tip,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Fun Fact Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Fun Fact",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            when {
                                viewModel.funFact.value.isLoading -> {
                                    CircularProgressIndicator()
                                }
                                viewModel.funFact.value.error != null -> {
                                    Text(
                                        text = viewModel.funFact.value.error ?: "No fun fact available for this location.",
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                viewModel.funFact.value.summary != null -> {
                                    viewModel.funFact.value.imageUrl?.let { imageUrl ->
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = "Fun Fact Image",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(
                                        text = viewModel.funFact.value.summary ?: "",
                                        color = TextGray,
                                        textAlign = TextAlign.Center,
                                        fontSize = 15.sp
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "No fun fact available for this location.",
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Article,
                            contentDescription = "News Icon",
                            tint = LeafGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Latest NASA News",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen
                        )
                    }
                    if (rssLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (rssError != null) {
                        Text(
                            text = rssError ?: "Error loading news",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        RssNewsList(rssItems)
                    }
                }
            }
        }
    }
}

@Composable
fun TemperatureGraph(
    temperatureHistory: List<TemperatureHistory>,
    modifier: Modifier = Modifier
) {
    if (temperatureHistory.isEmpty()) return
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 16.dp.toPx()
        
        val temperatures = temperatureHistory.map { it.value }
        val minTemp = temperatures.minOrNull() ?: 0.0
        val maxTemp = temperatures.maxOrNull() ?: 0.0
        val tempRange = (maxTemp - minTemp).coerceAtLeast(1.0)
        
        val xStep = (width - 2 * padding) / (temperatures.size - 1)
        val path = Path()
        
        temperatures.forEachIndexed { index, temp ->
            val x = padding + index * xStep
            val y = height - padding - ((temp - minTemp) / tempRange) * (height - 2 * padding)
            
            if (index == 0) {
                path.moveTo(x, y.toFloat())
            } else {
                path.lineTo(x, y.toFloat())
            }
            
            // Draw points
            drawCircle(
                color = TemperatureRed,
                radius = 4.dp.toPx(),
                center = Offset(x, y.toFloat())
            )
        }
        
        // Draw line
        drawPath(
            path = path,
            color = TemperatureRed,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun DataRow(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                color = TextGray,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GlowingLeafIcon(temperature: Double) {
    // Animate glow color based on temperature
    val targetColor = when {
        temperature < 20 -> LeafGreen
        temperature < 30 -> Color(0xFFFFF176) // Yellow
        else -> Color(0xFFE57373) // Red
    }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 800)
    )
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )
    Box(
        modifier = Modifier
            .padding(end = 16.dp)
            .size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = animatedColor.copy(alpha = glowAlpha),
                    shape = CircleShape
                )
        )
        Icon(
            imageVector = Icons.Default.Eco,
            contentDescription = "Glowing Leaf",
            tint = animatedColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

data class RssItem(val title: String, val pubDate: String, val link: String)

suspend fun fetchRss(url: String): List<RssItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<RssItem>()
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    val connection = URL(url).openConnection().apply {
        setRequestProperty("User-Agent", "Mozilla/5.0")
    }
    parser.setInput(connection.getInputStream(), null)
    var eventType = parser.eventType
    var title: String? = null
    var link: String? = null
    var pubDate: String? = null
    var currentTag: String? = null
    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                currentTag = parser.name
                if (currentTag.equals("item", ignoreCase = true)) {
                    title = null; link = null; pubDate = null
                }
            }
            XmlPullParser.TEXT -> {
                val text = parser.text
                when (currentTag) {
                    "title" -> title = text
                    "link" -> link = text
                    "pubDate" -> pubDate = text
                }
            }
            XmlPullParser.END_TAG -> {
                if (parser.name.equals("item", ignoreCase = true) && title != null && link != null && pubDate != null) {
                    items.add(RssItem(title, pubDate, link))
                }
                currentTag = null
            }
        }
        eventType = parser.next()
    }
    items
}

@Composable
fun RssNewsList(items: List<RssItem>) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .padding(horizontal = 16.dp)
            .background(LightGray, RoundedCornerShape(16.dp))
    ) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = { uriHandler.openUri(item.link) }
                        )
                        .padding(12.dp)
                ) {
                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = item.pubDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun NightSkyHeader() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(NightBlue)
    ) {
        // Moon
        Icon(
            imageVector = Icons.Default.Brightness2,
            contentDescription = "Moon",
            tint = MoonWhite,
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.TopEnd)
                .padding(12.dp)
        )
        // Stars
        repeat(8) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(StarYellow, CircleShape)
                    .offset(
                        x = (10..300).random().dp,
                        y = (5..50).random().dp
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvoTopBar(isDark: Boolean, onToggleDark: () -> Unit) {
    SmallTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = "Leaf Icon",
                    tint = LeafGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Envo", fontWeight = FontWeight.Bold)
            }
        },
        actions = {
            IconButton(onClick = onToggleDark) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle dark mode",
                    tint = LeafGreen
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun NasaNewsScreen() {
    val (isDark, setDarkMode) = rememberDarkModeState()
    var rssItems by remember { mutableStateOf<List<RssItem>>(emptyList()) }
    var rssLoading by remember { mutableStateOf(false) }
    var rssError by remember { mutableStateOf<String?>(null) }
    val rssUrl = "https://www.nasa.gov/rss/dyn/breaking_news.rss"
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(rssUrl) {
        rssLoading = true
        rssError = null
        try {
            rssItems = fetchRss(rssUrl)
        } catch (e: Exception) {
            rssError = "Failed to load news: ${e.localizedMessage}"
        } finally {
            rssLoading = false
        }
    }
    Scaffold(
        topBar = {
            EnvoTopBar(isDark = isDark) { setDarkMode(!isDark) }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (rssLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (rssError != null) {
                Text(
                    text = rssError ?: "Error loading news",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rssItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RectangleShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        onClick = { uriHandler.openUri(item.link) }
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = item.pubDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CarbonFootprintScreen() {
    val (isDark, setDarkMode) = rememberDarkModeState()
    Scaffold(
        topBar = {
            EnvoTopBar(isDark = isDark) { setDarkMode(!isDark) }
        }
    ) { innerPadding ->
        var transport by remember { mutableStateOf(0f) }
        var energy by remember { mutableStateOf(0f) }
        var food by remember { mutableStateOf(0f) }
        val total = transport * 0.2f + energy * 0.8f + food * 2.5f
        val animatedTotal by animateFloatAsState(targetValue = total, label = "carbonAnim")
        val tips = listOf(
            "Walk, bike, or use public transport whenever possible.",
            "Switch to renewable energy sources at home.",
            "Eat more plant-based meals.",
            "Reduce, reuse, and recycle.",
            "Conserve water and electricity.",
            "Buy local and seasonal products."
        )
        
        // Add scroll state
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(scrollState) // Make the entire content scrollable
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Carbon Footprint Calculator",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Enter your daily activities:", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    Text("Transport (km driven)", color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = transport,
                        onValueChange = { transport = it },
                        valueRange = 0f..100f,
                        steps = 99,
                        colors = SliderDefaults.colors(thumbColor = LeafGreen, activeTrackColor = LeafGreen)
                    )
                    Text("${transport.toInt()} km", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text("Energy (kWh used)", color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = energy,
                        onValueChange = { energy = it },
                        valueRange = 0f..100f,
                        steps = 99,
                        colors = SliderDefaults.colors(thumbColor = LeafGreen, activeTrackColor = LeafGreen)
                    )
                    Text("${energy.toInt()} kWh", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text("Food (meals with meat)", color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = food,
                        onValueChange = { food = it },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = LeafGreen, activeTrackColor = LeafGreen)
                    )
                    Text("${food.toInt()} meals", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Estimated Carbon Footprint:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = String.format("%.1f kg CO₂/day", animatedTotal),
                        style = MaterialTheme.typography.headlineMedium,
                        color = LeafGreen,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Tips to Reduce Your Carbon Footprint",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Tips section with glowing cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                tips.forEach { tip ->
                    val infiniteTransition = rememberInfiniteTransition(label = "glow")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "glowAlpha"
                    )
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) {
                                Color(0xFF1E3A5F).copy(alpha = glowAlpha)
                            } else {
                                Color(0xFFE8F5E9).copy(alpha = glowAlpha)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = "Eco Tip",
                                tint = if (isDark) Color(0xFF81C784) else LeafGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = tip,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun EcoScopeDashboardScreen(viewModel: EcoScopeViewModel = viewModel()) {
    val (isDark, setDarkMode) = rememberDarkModeState()
    Scaffold(
        topBar = {
            EnvoTopBar(isDark = isDark) { setDarkMode(!isDark) }
        }
    ) { innerPadding ->
        var searchQuery by remember { mutableStateOf("") }
        val context = LocalContext.current
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val pullRefreshState = rememberPullRefreshState(
            refreshing = viewModel.environmentalData.value.isLoading,
            onRefresh = {
                coroutineScope.launch {
                    viewModel.fetchEnvironmentalData(viewModel.currentLocation.value.latitude, viewModel.currentLocation.value.longitude)
                }
            }
        )
        val horizontalScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
                .pullRefresh(pullRefreshState)
                .padding(innerPadding)
        ) {
            // Location name and search bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = viewModel.locationName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for a location", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = LeafGreen,
                        cursorColor = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotEmpty()) {
                                try {
                                    viewModel.searchLocation(searchQuery, context)
                                } catch (e: Exception) {
                                    Log.e("EcoScopeDashboardScreen", "Error during search", e)
                                    Toast.makeText(context, "Search failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please enter a location", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ),
                    singleLine = true,
                    enabled = !viewModel.environmentalData.value.isLoading
                )
            }
            // Current Data Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Current Data",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (viewModel.environmentalData.value.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        if (viewModel.environmentalData.value.isEstimated) {
                            Text(
                                text = "⚠️ Using estimated data",
                                color = Color(0xFFFFA000),
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                            )
                        }
                        DataRow(
                            icon = Icons.Default.Thermostat,
                            title = "Air Temperature (2m above ground)",
                            value = String.format("%.1f°C", viewModel.environmentalData.value.temperature),
                            color = TemperatureRed
                        )
                        DataRow(
                            icon = Icons.Default.Cloud,
                            title = "Cloud Cover",
                            value = String.format("%.0f%%", viewModel.environmentalData.value.cloudCover),
                            color = Color(0xFF64B5F6)
                        )
                        DataRow(
                            icon = Icons.Default.WbSunny,
                            title = "Solar Radiation",
                            value = String.format("%.1f W/m²", viewModel.environmentalData.value.solarRadiation),
                            color = AerosolOrange
                        )
                    }
                }
            }
            // Environmental Tips Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Eco-Friendly Living Tips",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LeafGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .horizontalScroll(horizontalScrollState)
                        .fillMaxWidth()
                ) {
                    val environmentalTips = listOf(
                        "Use public transport or carpool to reduce carbon emissions",
                        "Switch to energy-efficient LED bulbs to save electricity",
                        "Reduce, reuse, and recycle to minimize waste",
                        "Plant trees and maintain green spaces in your community",
                        "Use reusable water bottles and shopping bags",
                        "Support local and sustainable food production",
                        "Turn off lights and unplug devices when not in use",
                        "Choose eco-friendly cleaning products",
                        "Conserve water by fixing leaks and using water-saving fixtures",
                        "Educate others about environmental conservation"
                    )
                    environmentalTips.forEach { tip ->
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = "Eco Tip",
                                    tint = LeafGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tip,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            // Fun Fact Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fun Fact",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    when {
                        viewModel.funFact.value.isLoading -> {
                            CircularProgressIndicator()
                        }
                        viewModel.funFact.value.error != null -> {
                            Text(
                                text = viewModel.funFact.value.error ?: "No fun fact available for this location.",
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                        viewModel.funFact.value.summary != null -> {
                            viewModel.funFact.value.imageUrl?.let { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Fun Fact Image",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = viewModel.funFact.value.summary ?: "",
                                color = TextGray,
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp
                            )
                        }
                        else -> {
                            Text(
                                text = "No fun fact available for this location.",
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
