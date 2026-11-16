/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.libremobileos.sidebar.ui.sidebar

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.app.SidebarApplication
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTokens
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTypography
import com.libremobileos.sidebar.ui.theme.SidebarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SidebarPerAppConfigFragment : Fragment() {

    companion object {
        const val PREF_AUTO_APPS = "sidebar_auto_apps"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SidebarTheme {
                    val activity = LocalContext.current as? Activity
                    SidebarPerAppConfigScreen(onBack = { activity?.finish() })
                }
            }
        }
    }
}

@Composable
fun SidebarPerAppConfigScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val haptics = rememberExpressiveHaptics()
    val sharedPrefs = remember { context.getSharedPreferences(SidebarApplication.CONFIG, Context.MODE_PRIVATE) }

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var enabledPackages by remember {
        mutableStateOf(sharedPrefs.getStringSet(SidebarPerAppConfigFragment.PREF_AUTO_APPS, emptySet())?.toSet() ?: emptySet())
    }
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by rememberSaveable { mutableStateOf(0) } // 0 = All, 1 = Enabled

    LaunchedEffect(Unit) {
        val loadedApps = withContext(Dispatchers.IO) {
            loadInstalledApps(context)
        }
        apps = loadedApps
    }

    val onToggleApp: (AppInfo, Boolean) -> Unit = { app, isChecked ->
        val updated = if (isChecked) {
            enabledPackages + app.packageName
        } else {
            enabledPackages - app.packageName
        }
        enabledPackages = updated
        haptics.toggle(isChecked)
        sharedPrefs.edit()
            .putStringSet(SidebarPerAppConfigFragment.PREF_AUTO_APPS, updated)
            .apply()
    }

    val filteredApps = remember(apps, searchQuery, filterMode, enabledPackages) {
        val baseList = if (filterMode == 1) {
            apps.filter { it.packageName in enabledPackages }
        } else {
            apps
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { app ->
                app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    CompositionLocalProvider(LocalNavController provides remember {
        object : NavControllerWrapper {
            override fun navigate(route: String, popUpCurrent: Boolean) {}
            override fun navigateBack() {
                haptics.contextClick()
                onBack()
            }
        }
    }) {
        SettingsScaffold(
            title = stringResource(R.string.sidebar_per_app_config)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeaderWithBadge(
                        title = stringResource(R.string.sidebar_per_app_config),
                        badgeLabel = stringResource(R.string.sidebar_per_app_badge_count, enabledPackages.size, apps.size),
                        badgeColor = MaterialTheme.colorScheme.primaryContainer,
                        badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = stringResource(R.string.sidebar_auto_enable_selected_apps_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    M3ExpressiveSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = stringResource(R.string.sidebar_per_app_search_placeholder)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpressiveFilterChip(
                            selected = filterMode == 0,
                            onClick = {
                                if (filterMode != 0) {
                                    haptics.segmentTick()
                                    filterMode = 0
                                }
                            },
                            label = stringResource(R.string.sidebar_per_app_filter_all, apps.size)
                        )
                        ExpressiveFilterChip(
                            selected = filterMode == 1,
                            onClick = {
                                if (filterMode != 1) {
                                    haptics.segmentTick()
                                    filterMode = 1
                                }
                            },
                            label = stringResource(R.string.sidebar_per_app_filter_enabled, enabledPackages.size)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = M3ExpressiveTokens.CornerLarge,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        stringResource(R.string.sidebar_per_app_no_matching, searchQuery)
                                    } else {
                                        stringResource(R.string.sidebar_per_app_none_enabled)
                                    },
                                    style = M3ExpressiveTypography.TitleMediumEmphasized,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        stringResource(R.string.sidebar_per_app_no_matching_subtitle)
                                    } else {
                                        stringResource(R.string.sidebar_per_app_none_enabled_subtitle)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        itemsIndexed(
                            items = filteredApps,
                            key = { _, app -> app.packageName }
                        ) { index, appInfo ->
                            val isFirst = index == 0
                            val isLast = index == filteredApps.lastIndex
                            val position = when {
                                isFirst && isLast -> ConnectedPosition.Single
                                isFirst -> ConnectedPosition.Top
                                isLast -> ConnectedPosition.Bottom
                                else -> ConnectedPosition.Middle
                            }
                            val isEnabled = appInfo.packageName in enabledPackages
                            SidebarPerAppListItem(
                                appInfo = appInfo,
                                isEnabled = isEnabled,
                                position = position,
                                showDivider = !isLast,
                                modifier = Modifier.animateItem(placementSpec = M3ExpressiveTokens.spatialFast<IntOffset>()),
                                onToggle = { checked ->
                                    onToggleApp(appInfo, checked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Connected list item with position-dependent corner radii.
 */
@Composable
fun SidebarPerAppListItem(
    appInfo: AppInfo,
    isEnabled: Boolean,
    position: ConnectedPosition = ConnectedPosition.Single,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    val shape = remember(position) { getConnectedShape(position) }
    val containerColor by animateColorAsState(
        targetValue = if (isEnabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "PerAppItemContainerColor"
    )

    Surface(
        color = containerColor,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .clickable { onToggle(!isEnabled) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberDrawablePainter(appInfo.icon),
                    contentDescription = appInfo.label,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(M3ExpressiveTokens.CornerSmall)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appInfo.label,
                        style = if (isEnabled) {
                            M3ExpressiveTypography.TitleSmallEmphasized
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Crossfade(
                        targetState = isEnabled,
                        animationSpec = M3ExpressiveTokens.effectsFast(),
                        label = "PerAppItemSubtitleCrossfade"
                    ) { active ->
                        Text(
                            text = if (active) stringResource(R.string.sidebar_per_app_item_active) else appInfo.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    thumbContent = if (isEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }

            if (showDivider) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp)
                )
            }
        }
    }
}

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

private suspend fun loadInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    
    apps.asSequence()
        .filter { app ->
            // Filter out system apps and the current app
            (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && 
            app.packageName != context.packageName
        }
        .map { app ->
            AppInfo(
                label = app.loadLabel(pm).toString(),
                packageName = app.packageName,
                icon = app.loadIcon(pm)
            )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}
