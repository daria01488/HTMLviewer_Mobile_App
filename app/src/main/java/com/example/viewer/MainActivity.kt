package com.example.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.mutableLongStateOf
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HtmlViewerApp()
            }
        }
    }
}

// --- Colors ---
val BgDark = Color(0xFF161616)
val SurfaceDark = Color(0xFF2A2A2A)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)

@Composable
fun HtmlViewerApp() {
    val context = LocalContext.current

    // State to hold the chosen file's URI and Name
    var currentUri by remember { mutableStateOf<Uri?>(null) }
    var currentFileName by remember { mutableStateOf<String?>(null) }

    // State to hold our history of recent files
    val recentFiles = remember { mutableStateListOf<RecentFile>() }

    // Helper function to handle opening a file and updating history
    fun openFileAndUpdateHistory(uri: Uri, fileName: String) {
        currentUri = uri
        currentFileName = fileName

        // 1. Remove it if it's already in the list (so we can move it to the top)
        recentFiles.removeAll { it.uri == uri }

        // 2. Add it to the very top (index 0) with the current exact time
        recentFiles.add(0, RecentFile(uri, fileName, System.currentTimeMillis()))

        // 3. Keep only the last 3 files
        if (recentFiles.size > 3) {
            recentFiles.removeAt(recentFiles.size - 1)
        }
    }

    // This handles the Android hardware back button
    BackHandler(enabled = currentFileName != null) {
        currentUri = null
        currentFileName = null
    }

    // The File Picker Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Ask Android to let us keep permission to read this file later!
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            openFileAndUpdateHistory(uri, getFileName(context, uri))
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDark
    ) {
        if (currentFileName == null) {
            UploadScreen(
                recentFiles = recentFiles,
                onLaunchPicker = {
                    // Launch the picker filtering for HTML/Text files
                    launcher.launch(arrayOf("text/html", "text/plain"))
                },
                onRecentFileSelected = { file ->
                    // Open the file directly from the recent history
                    openFileAndUpdateHistory(file.uri, file.name)
                }
            )
        } else {
            ViewerScreen(
                fileName = currentFileName!!,
                fileUri = currentUri,
                onBack = {
                    currentUri = null
                    currentFileName = null
                },
                onClick = {
                    launcher.launch(arrayOf("text/html", "text/plain"))
                }
            )
        }
    }
}

@Composable
fun UploadScreen(
    recentFiles: List<RecentFile>,
    onLaunchPicker: () -> Unit,
    onRecentFileSelected: (RecentFile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // App Header
        HeaderSection()

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Upload File to View HTML",
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Upload Area with Dashed Border
        UploadBox(onClick = onLaunchPicker)

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Files Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RECENT FILES",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Check if our history is empty or not
        if (recentFiles.isEmpty()) {
            Text(
                text = "No recent files",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recentFiles) { file ->
                    RecentFileCard(file = file) { onRecentFileSelected(file) }
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
            text = "HTML Viewer",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UploadBox(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .drawBehind {
                val stroke = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                )
                drawRoundRect(
                    color = SurfaceDark,
                    style = stroke,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.custom_upload_icon),
                modifier = Modifier.size(32.dp),
                contentDescription = "Upload",
                tint = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose a Document",
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "HTML, HTM files supported",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun RecentFileCard(file: RecentFile, onClick: () -> Unit) {
    // 1. Create a state variable that holds the current time.
    // Because it's a "State", changing it will force the card to redraw.
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 2. Start a background loop that runs as long as this card is visible
    LaunchedEffect(Unit) {
        while (true) {
            // Wait for 60,000 milliseconds (1 minute)
            kotlinx.coroutines.delay(60_000L)
            // Update the state with the new actual time
            currentTimeMs = System.currentTimeMillis()
        }
    }

    // 3. Calculate the time string using our live-updating currentTimeMs
    val timeString = DateUtils.getRelativeTimeSpanString(
        file.timeMs,
        currentTimeMs,
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = file.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = timeString, // This will now automatically change!
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ViewerScreen(fileName: String, fileUri: Uri?, onBack: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current

    val htmlContent = remember(fileUri) {
        if (fileUri != null) {
            try {
                context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use {
                    it.readText()
                } ?: "Error: Could not read file."
            } catch (e: Exception) {
                "Error reading file: ${e.message}"
            }
        } else {
            "<h1>File Error</h1><p>No valid URI provided.</p>"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(modifier = Modifier.padding(24.dp, 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .padding(12.dp)
                        .clickable { onBack() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.custom_computer_icon),
                        modifier = Modifier.size(24.dp),
                        contentDescription = "Desktop Icon",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .clickable { onClick() }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fileName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
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
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// --- Helper Functions & Classes ---

// Updated data class to hold the Uri and the precise timestamp
data class RecentFile(val uri: Uri, val name: String, val timeMs: Long)

@SuppressLint("Range")
fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result ?: "Unknown File"
}