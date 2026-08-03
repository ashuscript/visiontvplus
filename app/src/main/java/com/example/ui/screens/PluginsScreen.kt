package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.NuvioScraper
import com.example.data.repository.ProviderCategory
import com.example.data.repository.ProviderItem
import com.example.ui.theme.*

@Composable
fun PluginsScreen(
    providers: List<ProviderItem>,
    plugins: List<NuvioScraper>,
    isLoadingPlugins: Boolean,
    onBackClick: (() -> Unit)? = null,
    onAddProvider: ((url: String, category: ProviderCategory, name: String?) -> Unit)? = null,
    onToggleProvider: ((providerId: String, isEnabled: Boolean) -> Unit)? = null,
    onRemoveProvider: ((providerId: String) -> Unit)? = null,
    onTestProvider: ((providerId: String) -> Unit)? = null,
    onAddRepo: ((url: String, name: String?) -> Unit)? = null,
    onDeleteRepo: ((repoId: String) -> Unit)? = null,
    onRefreshRepo: ((repoId: String) -> Unit)? = null,
    onTogglePlugin: (pluginId: String, isEnabled: Boolean) -> Unit,
    onTogglePinPlugin: (pluginId: String, isPinned: Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Stremio Addons, 2: Scraper Plugins
    var newExtensionUrl by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val stremioAddons = remember(providers) { providers.filter { it.category == ProviderCategory.STREMIO_ADDON } }
    val nuvioRepos = remember(providers) { providers.filter { it.category == ProviderCategory.NUVIO_PLUGIN } }

    val filteredPlugins = remember(plugins, searchQuery) {
        if (searchQuery.isBlank()) {
            plugins.sortedByDescending { it.isPinned }
        } else {
            plugins.filter { plugin ->
                plugin.name.contains(searchQuery, ignoreCase = true) ||
                        (plugin.description?.contains(searchQuery, ignoreCase = true) == true) ||
                        (plugin.author?.contains(searchQuery, ignoreCase = true) == true)
            }.sortedByDescending { it.isPinned }
        }
    }

    fun autoDetectCategory(rawUrl: String): ProviderCategory {
        val lower = rawUrl.lowercase().trim()
        return when {
            lower.contains("stremio") || lower.contains("manifest.json") || lower.contains("torrentio") ||
                    lower.contains("cyberflix") || lower.contains("cinemeta") || lower.contains("tengu") ||
                    lower.contains("pengu") -> ProviderCategory.STREMIO_ADDON
            else -> ProviderCategory.NUVIO_PLUGIN
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D14))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column {
                Text(
                    text = "Addons & Scrapers",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Manage active streaming sources and scrapers",
                    fontSize = 12.sp,
                    color = Color(0x88FFFFFF)
                )
            }
        }

        // Add Extension / Manifest Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141720))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Add Stremio Addon or Scraper Manifest",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2230))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (newExtensionUrl.isEmpty()) {
                        Text(
                            text = "Paste manifest.json or addon URL...",
                            color = Color(0x66FFFFFF),
                            fontSize = 12.sp
                        )
                    }
                    BasicTextField(
                        value = newExtensionUrl,
                        onValueChange = { newExtensionUrl = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(AppleTvAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (newExtensionUrl.isEmpty()) {
                    IconButton(
                        onClick = {
                            clipboardManager.getText()?.text?.let { text ->
                                newExtensionUrl = text.toString()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = AppleTvAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { newExtensionUrl = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0x88FFFFFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppleTvAccent)
                        .clickable {
                            val urlToInstall = newExtensionUrl.trim()
                            if (urlToInstall.isBlank()) {
                                Toast.makeText(context, "Please enter a manifest URL", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }
                            val category = autoDetectCategory(urlToInstall)
                            if (onAddProvider != null) {
                                onAddProvider(urlToInstall, category, null)
                            } else onAddRepo?.invoke(urlToInstall, null)

                            newExtensionUrl = ""
                            Toast.makeText(context, "Addon added successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("Add", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141720))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("All (${providers.size + plugins.size})", "Stremio Addons (${stremioAddons.size})", "Plugins (${plugins.size})")
            tabs.forEachIndexed { idx, title ->
                val isSel = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) AppleTvAccent else Color.Transparent)
                        .clickable { selectedTab = idx }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.Black else Color(0xAAFFFFFF),
                        maxLines = 1
                    )
                }
            }
        }

        // Lazy List of Addons and Plugins
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Stremio / Custom Providers
            if (selectedTab == 0 || selectedTab == 1) {
                if (stremioAddons.isNotEmpty() || nuvioRepos.isNotEmpty()) {
                    items(stremioAddons + nuvioRepos, key = { "provider_${it.id}" }) { provider ->
                        SimpleProviderCard(
                            item = provider,
                            onToggle = { enabled -> onToggleProvider?.invoke(provider.id, enabled) },
                            onRemove = { onRemoveProvider?.invoke(provider.id) }
                        )
                    }
                }
            }

            // Scraper Plugins
            if (selectedTab == 0 || selectedTab == 2) {
                if (plugins.isNotEmpty()) {
                    item(key = "search_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141720))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0x66FFFFFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("Filter scraper plugins...", color = Color(0x66FFFFFF), fontSize = 12.sp)
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                    singleLine = true,
                                    cursorBrush = SolidColor(AppleTvAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0x88FFFFFF), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    if (isLoadingPlugins) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AppleTvAccent, modifier = Modifier.size(24.dp))
                            }
                        }
                    } else {
                        items(filteredPlugins, key = { "plugin_${it.id}" }) { plugin ->
                            SimplePluginCard(
                                plugin = plugin,
                                onToggle = { enabled -> onTogglePlugin(plugin.id, enabled) },
                                onTogglePin = { pinned -> onTogglePinPlugin(plugin.id, pinned) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleProviderCard(
    item: ProviderItem,
    onToggle: (Boolean) -> Unit,
    onRemove: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141720))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (item.category == ProviderCategory.STREMIO_ADDON) "Stremio" else "Scraper",
                        fontSize = 9.sp,
                        color = Color(0xDDFFFFFF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.url,
                fontSize = 11.sp,
                color = Color(0x77FFFFFF),
                maxLines = 1
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Switch(
                checked = item.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppleTvAccent,
                    uncheckedThumbColor = Color(0x88FFFFFF),
                    uncheckedTrackColor = Color(0x22FFFFFF)
                )
            )
        }
    }
}

@Composable
fun SimplePluginCard(
    plugin: NuvioScraper,
    onToggle: (Boolean) -> Unit,
    onTogglePin: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141720))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2230)),
            contentAlignment = Alignment.Center
        ) {
            if (!plugin.logo.isNullOrEmpty()) {
                AsyncImage(
                    model = plugin.logo,
                    contentDescription = plugin.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = plugin.name.take(2).uppercase(),
                    color = AppleTvAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plugin.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = plugin.description ?: plugin.author ?: "Native Scraper",
                fontSize = 11.sp,
                color = Color(0x77FFFFFF),
                maxLines = 1
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onTogglePin(!plugin.isPinned) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (plugin.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = if (plugin.isPinned) AppleTvAccent else Color(0x44FFFFFF),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Switch(
                checked = plugin.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppleTvAccent,
                    uncheckedThumbColor = Color(0x88FFFFFF),
                    uncheckedTrackColor = Color(0x22FFFFFF)
                )
            )
        }
    }
}
