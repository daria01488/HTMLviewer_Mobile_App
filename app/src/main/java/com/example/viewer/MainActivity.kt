package com.example.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BrowserApp()
            }
        }
    }
}

// --- Colors ---
val BgDark = Color(0xFF161616)
val SurfaceDark = Color(0xFF2A2A2A)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
val AccentBlue = Color(0xFF4A90E2)

@Composable
fun BrowserApp() {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf<String?>(null) }

    // Initialize list by reading from the phone's saved storage
    val recentSearches = remember {
        mutableStateListOf<RecentSearch>().apply {
            addAll(HistoryManager.loadHistory(context))
        }
    }

    // Smart logic to handle Google searches vs direct URLs
    fun openUrlAndUpdateHistory(input: String) {
        if (input.isBlank()) return
        val cleanInput = input.trim()
        val looksLikeUrl = cleanInput.contains(".") && !cleanInput.contains(" ")

        val finalUrl = when {
            looksLikeUrl && !cleanInput.startsWith("http://") && !cleanInput.startsWith("https://") -> {
                "https://$cleanInput"
            }
            looksLikeUrl -> cleanInput
            else -> {
                val encodedQuery = URLEncoder.encode(cleanInput, "UTF-8")
                "https://www.google.com/search?q=$encodedQuery"
            }
        }

        currentUrl = finalUrl

        // Update history order
        recentSearches.removeAll { it.url == finalUrl }
        recentSearches.add(0, RecentSearch(finalUrl, System.currentTimeMillis()))

        // Keep only top 5 searches
        if (recentSearches.size > 5) {
            recentSearches.removeAt(recentSearches.size - 1)
        }

        // Save updated list to phone storage
        HistoryManager.saveHistory(context, recentSearches)
    }

    // Handle hardware back button
    BackHandler(enabled = currentUrl != null) {
        currentUrl = null
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(), // Ensures UI is below status bar and above nav bar
        color = BgDark,
    ) {
        if (currentUrl == null) {
            SearchScreen(
                recentSearches = recentSearches,
                onSearch = { url ->
                    openUrlAndUpdateHistory(url)
                }
            )
        } else {
            ViewerScreen(
                url = currentUrl!!,
                onBack = { currentUrl = null },
                onSearch = { newUrl ->
                    openUrlAndUpdateHistory(newUrl)
                }
            )
        }
    }
}

@Composable
fun SearchScreen(
    recentSearches: List<RecentSearch>,
    onSearch: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var searchInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        HeaderSection()

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter a web address to browse",
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = searchInput,
            onValueChange = { searchInput = it },
            placeholder = { Text("e.g. google.com", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = SurfaceDark,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                cursorColor = AccentBlue
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    onSearch(searchInput)
                }
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onSearch(searchInput)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text("Browse", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RECENT SEARCHES",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (recentSearches.isEmpty()) {
            Text(
                text = "No recent searches",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recentSearches) { search ->
                    RecentSearchCard(search = search) {
                        onSearch(search.url)
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.custom_description_icon),
            modifier = Modifier.size(32.dp),
            contentDescription = "App Icon",
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Web Viewer",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecentSearchCard(search: RecentSearch, onClick: () -> Unit) {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Live updating background timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val timeString = DateUtils.getRelativeTimeSpanString(
        search.timeMs,
        currentTimeMs,
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
            .padding(16.dp, 16.dp), // Fixed padding to look better
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = search.url.replace("https://www.google.com/search?q=", "Search: ")
                    .replace("https://", "")
                    .replace("http://", "")
                    .replace("+", " "),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = timeString,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ViewerScreen(url: String, onBack: () -> Unit, onSearch: (String) -> Unit) {
    val focusManager = LocalFocusManager.current

    // Helper function to make URLs look clean in the address bar
    fun formatDisplayUrl(raw: String): String {
        // If it's a Google search, extract just the search term
        if (raw.contains("google.com/search?q=")) {
            val qStart = raw.indexOf("q=") + 2
            val qEnd = raw.indexOf("&", qStart).takeIf { it != -1 } ?: raw.length
            return try {
                java.net.URLDecoder.decode(raw.substring(qStart, qEnd), "UTF-8")
            } catch (e: Exception) { raw }
        }
        // Otherwise, just remove the https://
        return raw.replace("https://", "").replace("http://", "")
    }

    // State for the text in the search bar
    var searchInput by remember(url) { mutableStateOf(formatDisplayUrl(url)) }

    // State to prevent the WebView from getting stuck in an infinite reload loop
    var lastLoadedUrl by remember { mutableStateOf(url) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(modifier = Modifier.padding(16.dp, 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .clickable { onBack() }
                        .padding(14.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.custom_computer_icon),
                        modifier = Modifier.size(24.dp),
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Interactive Address Bar
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    modifier = Modifier.weight(1f).height(54.dp), // Slim height
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        cursorColor = AccentBlue
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            focusManager.clearFocus()
                            onSearch(searchInput)
                        }
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        // THE FIX: This custom client listens for page changes
                        webViewClient = object : WebViewClient() {
                            override fun doUpdateVisitedHistory(view: WebView, loadedUrl: String, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, loadedUrl, isReload)
                                // Automatically sync the top address bar with the new internal page!
                                searchInput = formatDisplayUrl(loadedUrl)
                            }
                        }
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    // Only tell the WebView to load if the user hit "Go" on their keyboard
                    if (url != lastLoadedUrl) {
                        webView.loadUrl(url)
                        lastLoadedUrl = url
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
// --- Data Classes & Persistence Helpers ---

data class RecentSearch(val url: String, val timeMs: Long)

object HistoryManager {
    private const val PREFS_NAME = "browser_prefs"
    private const val KEY_HISTORY = "recent_searches"

    fun saveHistory(context: Context, history: List<RecentSearch>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyString = history.joinToString(",") { "${it.url}|${it.timeMs}" }
        prefs.edit().putString(KEY_HISTORY, historyString).apply()
    }

    fun loadHistory(context: Context): List<RecentSearch> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyString = prefs.getString(KEY_HISTORY, "") ?: ""
        if (historyString.isEmpty()) return emptyList()

        return historyString.split(",").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) {
                val url = parts[0]
                val time = parts[1].toLongOrNull()
                if (time != null) RecentSearch(url, time) else null
            } else null
        }
    }
}