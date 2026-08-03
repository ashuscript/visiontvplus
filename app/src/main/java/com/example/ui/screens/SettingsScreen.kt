package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ProviderCategory
import com.example.data.repository.ProviderItem
import com.example.data.repository.ProviderTestResult
import com.example.ui.theme.AppleTvAccent

@Composable
fun SettingsScreen(
    providers: List<ProviderItem>,
    onTestProvider: (id: String) -> Unit,
    onAddProvider: (url: String, category: ProviderCategory, name: String?, onResult: (ProviderTestResult) -> Unit) -> Unit,
    onRemoveProvider: (id: String) -> Unit,
    onToggleProvider: (id: String, enabled: Boolean) -> Unit,
    onOpenPluginsScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inputUrl by remember { mutableStateOf("") }
    var isTestingAndAdding by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, start = 20.dp, end = 20.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Screen Title Header
            item(key = "header") {
                Text(
                    text = "Settings",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // CONTENT & PROVIDERS SECTION
            item(key = "providers_section") {
                Column {
                    Text(
                        text = "CONTENT & PLUGINS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0x88FFFFFF),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141720))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = AppleTvAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Scraper & Stremio Addons",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${providers.size} active provider source(s)",
                                        fontSize = 12.sp,
                                        color = Color(0x88FFFFFF)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (onOpenPluginsScreen != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppleTvAccent)
                                        .clickable { onOpenPluginsScreen() }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "Manage",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Add Manifest URL
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E2230))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = inputUrl,
                                onValueChange = { inputUrl = it },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                cursorBrush = SolidColor(AppleTvAccent),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (inputUrl.isEmpty()) {
                                        Text("Paste Stremio Addon manifest URL...", color = Color(0x66FFFFFF), fontSize = 13.sp)
                                    }
                                    inner()
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isTestingAndAdding) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppleTvAccent)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppleTvAccent)
                                        .clickable {
                                            if (inputUrl.isNotBlank()) {
                                                isTestingAndAdding = true
                                                onAddProvider(inputUrl, ProviderCategory.STREMIO_ADDON, null) { res ->
                                                    isTestingAndAdding = false
                                                    Toast.makeText(context, res.errorMessage ?: "Addon added successfully!", Toast.LENGTH_SHORT).show()
                                                    if (res.isSuccess) inputUrl = ""
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Add", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (providers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Installed Providers",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xAAFFFFFF)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            providers.forEachIndexed { idx, p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text(p.url, color = Color(0x77FFFFFF), fontSize = 11.sp, maxLines = 1)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onRemoveProvider(p.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Provider",
                                                tint = Color(0xFFFF5252),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Switch(
                                            checked = p.isEnabled,
                                            onCheckedChange = { onToggleProvider(p.id, it) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppleTvAccent)
                                        )
                                    }
                                }

                                if (idx < providers.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0x11FFFFFF))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ABOUT SECTION
            item(key = "about_section") {
                Column {
                    Text(
                        text = "APPLICATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0x88FFFFFF),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141720))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AppleTvAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Vision TV+",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Version 1.0.0-beta1 (Build 1)",
                                    fontSize = 12.sp,
                                    color = Color(0x88FFFFFF)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
