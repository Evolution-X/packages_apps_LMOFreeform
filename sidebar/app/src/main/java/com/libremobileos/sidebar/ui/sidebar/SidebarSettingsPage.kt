/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.libremobileos.sidebar.ui.sidebar

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.service.SidebarMonitorService
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SidebarSettingsPage(
    viewModel: SidebarSettingsViewModel
) {
    val context = LocalContext.current
    val haptics = rememberExpressiveHaptics()
    var mainChecked by rememberSaveable { mutableStateOf(viewModel.getSidebarEnabled()) }
    var autoEnableChecked by rememberSaveable { mutableStateOf(viewModel.getAutoEnableSelectedAppsEnabled()) }
    var predictedAppsChecked by rememberSaveable { mutableStateOf(viewModel.getPredictedAppsEnabled()) }
    var freeformMode by rememberSaveable { mutableStateOf(viewModel.getFreeformControlMode()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterMode by rememberSaveable { mutableStateOf(0) } // 0 = All apps, 1 = Pinned only

    val sidebarApps by viewModel.appListFlow.collectAsState()
    val pinnedApps = remember(sidebarApps) { sidebarApps.filter { it.isSidebarApp } }
    val pinnedCount = pinnedApps.size

    val filteredApps = remember(sidebarApps, searchQuery, filterMode) {
        val baseList = if (filterMode == 1) {
            sidebarApps.filter { it.isSidebarApp }
        } else {
            sidebarApps
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.sidebar_label),
        actions = {
            IconButton(onClick = {
                haptics.contextClick()
                context.startActivity(Intent(context, SidebarCustomizationActivity::class.java))
            }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.sidebar_customization_title)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HeroMasterSwitchCard(
                    checked = mainChecked,
                    enabled = viewModel.isEnabled,
                    onCheckedChange = { checked ->
                        mainChecked = checked
                        viewModel.setSidebarEnabled(checked)
                        val intent = Intent(context, SidebarMonitorService::class.java)
                        if (checked || autoEnableChecked) {
                            context.startService(intent)
                        } else {
                            context.stopService(intent)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                CustomizationGatewayCard(
                    onClick = {
                        context.startActivity(Intent(context, SidebarCustomizationActivity::class.java))
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                FreeformModeCard(
                    currentMode = freeformMode,
                    onModeChange = { mode ->
                        freeformMode = mode
                        viewModel.setFreeformControlMode(mode)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                SmartAutomationCard(
                    mainEnabled = mainChecked,
                    autoEnableChecked = autoEnableChecked,
                    onAutoEnableChange = { checked ->
                        autoEnableChecked = checked
                        viewModel.setAutoEnableSelectedAppsEnabled(checked)
                        val intent = Intent(context, SidebarMonitorService::class.java)
                        if (checked || mainChecked) {
                            context.startService(intent)
                        } else {
                            context.stopService(intent)
                        }
                    },
                    predictedAppsChecked = predictedAppsChecked,
                    onPredictedAppsChange = { checked ->
                        predictedAppsChecked = checked
                        viewModel.setPredictedAppsEnabled(checked)
                    },
                    onConfigureAppsClick = {
                        context.startActivity(Intent(context, SidebarPerAppConfigActivity::class.java))
                    }
                )
                if (mainChecked) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            item {
                AnimatedVisibility(
                    visible = mainChecked,
                    enter = expandVertically(animationSpec = M3ExpressiveTokens.spatialDefault()) +
                            fadeIn(animationSpec = M3ExpressiveTokens.effectsDefault()),
                    exit = shrinkVertically(animationSpec = M3ExpressiveTokens.spatialDefault()) +
                            fadeOut(animationSpec = M3ExpressiveTokens.effectsDefault())
                ) {
                    Column {
                        PinnedAppsDockShelf(
                            pinnedApps = pinnedApps,
                            onUnpin = { appInfo ->
                                viewModel.deleteSidebarApp(appInfo)
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = mainChecked,
                    enter = expandVertically(animationSpec = M3ExpressiveTokens.spatialDefault()) +
                            fadeIn(animationSpec = M3ExpressiveTokens.effectsDefault()),
                    exit = shrinkVertically(animationSpec = M3ExpressiveTokens.spatialDefault()) +
                            fadeOut(animationSpec = M3ExpressiveTokens.effectsDefault())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeaderWithBadge(
                            title = stringResource(R.string.sidebar_app_setting_label),
                            badgeLabel = stringResource(R.string.apps_count_badge, sidebarApps.size),
                            badgeColor = MaterialTheme.colorScheme.primaryContainer,
                            badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        M3ExpressiveSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = stringResource(R.string.sidebar_search_apps_to_pin)
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
                                label = stringResource(R.string.sidebar_filter_all, sidebarApps.size)
                            )
                            ExpressiveFilterChip(
                                selected = filterMode == 1,
                                onClick = {
                                    if (filterMode != 1) {
                                        haptics.segmentTick()
                                        filterMode = 1
                                    }
                                },
                                label = stringResource(R.string.sidebar_filter_pinned, pinnedCount)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

            if (mainChecked) {
                if (filteredApps.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = M3ExpressiveTokens.CornerLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        stringResource(R.string.sidebar_no_apps_found_matching, searchQuery)
                                    } else {
                                        stringResource(R.string.sidebar_no_pinned_apps_yet)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = filteredApps,
                        key = { _, it -> "${it.packageName}_${it.activityName}_${it.userId}" }
                    ) { index, appInfo ->
                        val isFirst = index == 0
                        val isLast = index == filteredApps.lastIndex
                        val position = when {
                            isFirst && isLast -> ConnectedPosition.Single
                            isFirst -> ConnectedPosition.Top
                            isLast -> ConnectedPosition.Bottom
                            else -> ConnectedPosition.Middle
                        }
                        SidebarAppListItem(
                            appInfo = appInfo,
                            position = position,
                            showDivider = !isLast,
                            modifier = Modifier.animateItem(placementSpec = M3ExpressiveTokens.spatialFast<IntOffset>()),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    viewModel.addSidebarApp(appInfo)
                                } else {
                                    viewModel.deleteSidebarApp(appInfo)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
