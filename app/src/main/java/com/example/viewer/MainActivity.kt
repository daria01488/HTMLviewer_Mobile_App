package com.example.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
            currentUri = uri
            currentFileName = getFileName(context, uri)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDark
    ) {
        if (currentFileName == null) {
            UploadScreen(
                onLaunchPicker = {
                    // Launch the picker filtering for HTML/Text files
                    launcher.launch(arrayOf("text/html", "text/plain"))
                },
                onDemoFileSelected = { fileName ->
                    // Fallback for your recent files dummy data
                    currentUri = null
                    currentFileName = fileName
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
                    // THE FIX: This tells the filename pill to open the file picker!
                    launcher.launch(arrayOf("text/html", "text/plain"))
                }
            )
        }
    }
}

@Composable
fun UploadScreen(onLaunchPicker: () -> Unit, onDemoFileSelected: (String) -> Unit) {
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

        val recentFiles = listOf(
            RecentFile("landing-page.html", "30m ago"),
            RecentFile("project-spec.pdf", "2h ago"),
            RecentFile("newsletter.html", "1d ago")
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recentFiles) { file ->
                RecentFileCard(file) { onDemoFileSelected(file.name) }
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
                text = file.time,
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

    // Read the HTML content from the URI, or use the demo if no URI exists
    val htmlContent = remember(fileUri) {
        if (fileUri != null) {
            try {
                // Open the file and read it into a String
                context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use {
                    it.readText()
                } ?: "Error: Could not read file."
            } catch (e: Exception) {
                "Error reading file: ${e.message}"
            }
        } else {
            // Fallback string for dummy "Recent Files" clicks
            """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; padding: 20px; color: #333; background-color: #FDF9F1;">
                <h1 style="font-size: 28px;">Sample Document<br>Title - About Us</h1>
                <p>This is a sample header. Your sample site and develop our inourant evort iour business. Our team portrait, information skills and biea and compares from presint team.</p>
                <div style="background-color: #e0e0e0; height: 180px; width: 100%; margin: 20px 0; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #888;">
                    [Team Photo Placeholder]
                </div>
                <ul>
                    <li>Our teamesting team</li>
                    <li>Our support cilient teams</li>
                    <li>Descuvors and communites</li>
                </ul>
                <button style="background-color: #007BFF; color: white; border: none; padding: 12px 24px; border-radius: 6px; font-weight: bold; margin-top: 20px;">CONTACT</button>
            </body>
            </html>
            """.trimIndent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Header
        Column(modifier = Modifier.padding(24.dp, 16.dp)) {

            // Toolbar (Filename pill and desktop icon)
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
                    // Limit filename length so it doesn't break UI
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

data class RecentFile(val name: String, val time: String)

/**
 * Safely extracts the real file name from an Android Content URI
 */
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