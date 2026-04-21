package com.example.viewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.getValue // REQUIRED for 'by remember'
import androidx.compose.runtime.setValue // REQUIRED for 'by remember'
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.viewer.R

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
    // State to toggle between the upload screen and the viewer screen
    var currentFile by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDark
    ) {
        if (currentFile == null) {
            UploadScreen(onFileSelected = { fileName ->
                currentFile = fileName
            })
        } else {
            ViewerScreen(
                fileName = currentFile!!,
                onBack = { currentFile = null }
            )
        }
    }
}

@Composable
fun UploadScreen(onFileSelected: (String) -> Unit) {
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
        UploadBox(onClick = { onFileSelected("landing-page.html") })

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

        Spacer(modifier = Modifier.height(8.dp))

        val recentFiles = listOf(
            RecentFile("landing-page.html", "30m ago"),
            RecentFile("project-spec.pdf", "2h ago"),
            RecentFile("newsletter.html", "1d ago")
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recentFiles) { file ->
                RecentFileCard(file) { onFileSelected(file.name) }
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
            tint = Color.Unspecified // Ensures your custom colored PNG shows correctly
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
                modifier = Modifier.size(40.dp),
                contentDescription = "Upload",
                tint = TextPrimary // Keeps this icon white
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
fun ViewerScreen(fileName: String, onBack: () -> Unit) {
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
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .clickable { onBack() }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = fileName, color = TextPrimary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .padding(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.custom_computer_icon),
                        modifier = Modifier.size(24.dp),
                        contentDescription = "Desktop Icon",
                        tint = TextPrimary // Keeps this icon white
                    )
                }
            }
        }

        val htmlContent = """
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

        // THE FIX: Changed from fillMaxSize() to weight(1f).fillMaxWidth()
        // to prevent the WebView from breaking layout bounds inside the Column
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
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

data class RecentFile(val name: String, val time: String)