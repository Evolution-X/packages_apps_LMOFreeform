/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.libremobileos.sidebar.ui.sidebar

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.bean.SidebarAppInfo
import com.libremobileos.sidebar.ui.sidebar.SidebarSettingsViewModel.Companion.FREEFORM_BUTTON_MODE
import com.libremobileos.sidebar.ui.sidebar.SidebarSettingsViewModel.Companion.FREEFORM_GESTURE_MODE
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTokens
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTypography

@Composable
fun HeroMasterSwitchCard(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    val containerColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "HeroMasterContainerColor"
    )

    Surface(
        color = containerColor,
        shape = M3ExpressiveTokens.CornerExtraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(M3ExpressiveTokens.CornerExtraLarge)
            .clickable(enabled = enabled) {
                val newChecked = !checked
                haptics.toggle(newChecked)
                onCheckedChange(newChecked)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.enable_sideline),
                        style = M3ExpressiveTypography.TitleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Crossfade(
                        targetState = checked,
                        animationSpec = M3ExpressiveTokens.effectsFast(),
                        label = "MasterBadgeCrossfade"
                    ) { isRunning ->
                        Surface(
                            color = if (isRunning) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            shape = M3ExpressiveTokens.CornerFull
                        ) {
                            Text(
                                text = if (isRunning) stringResource(R.string.status_running) else stringResource(R.string.status_off),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.sidebar_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = checked,
                onCheckedChange = { newChecked ->
                    haptics.toggle(newChecked)
                    onCheckedChange(newChecked)
                },
                enabled = enabled,
                thumbContent = if (checked) {
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
    }
}

@Composable
fun CustomizationGatewayCard(
    onClick: () -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = M3ExpressiveTokens.CornerLargeIncreased,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(M3ExpressiveTokens.CornerLargeIncreased)
            .clickable {
                haptics.contextClick()
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = M3ExpressiveTokens.CornerMedium,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sidebar_customization_title),
                    style = M3ExpressiveTypography.TitleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.sidebar_customization_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = M3ExpressiveTokens.CornerFull
            ) {
                Text(
                    text = stringResource(R.string.action_edit),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FreeformModeCard(
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = M3ExpressiveTokens.CornerLargeIncreased,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.sidebar_freeform_control_mode),
                style = M3ExpressiveTypography.TitleSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isGestureSelected = currentMode.equals(FREEFORM_GESTURE_MODE, ignoreCase = true)
                val isButtonSelected = currentMode.equals(FREEFORM_BUTTON_MODE, ignoreCase = true)

                val gestureShape = RoundedCornerShape(
                    topStart = 20.dp,
                    bottomStart = 20.dp,
                    topEnd = 4.dp,
                    bottomEnd = 4.dp
                )
                val buttonShape = RoundedCornerShape(
                    topStart = 4.dp,
                    bottomStart = 4.dp,
                    topEnd = 20.dp,
                    bottomEnd = 20.dp
                )

                ExpressiveConnectedButton(
                    text = stringResource(R.string.sidebar_freeform_control_gesture),
                    isSelected = isGestureSelected,
                    shape = gestureShape,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChange(FREEFORM_GESTURE_MODE) }
                )

                ExpressiveConnectedButton(
                    text = stringResource(R.string.sidebar_freeform_control_button),
                    isSelected = isButtonSelected,
                    shape = buttonShape,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChange(FREEFORM_BUTTON_MODE) }
                )
            }
        }
    }
}

@Composable
fun SmartAutomationCard(
    mainEnabled: Boolean,
    autoEnableChecked: Boolean,
    onAutoEnableChange: (Boolean) -> Unit,
    predictedAppsChecked: Boolean,
    onPredictedAppsChange: (Boolean) -> Unit,
    onConfigureAppsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = M3ExpressiveTokens.CornerLargeIncreased,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ExpressiveSwitchRow(
                title = stringResource(R.string.sidebar_auto_enable_selected_apps),
                summary = stringResource(R.string.sidebar_auto_enable_selected_apps_summary),
                checked = autoEnableChecked,
                onCheckedChange = onAutoEnableChange
            )

            AnimatedVisibility(
                visible = autoEnableChecked,
                enter = expandVertically(animationSpec = M3ExpressiveTokens.spatialFast()) +
                        fadeIn(animationSpec = M3ExpressiveTokens.effectsFast()),
                exit = shrinkVertically(animationSpec = M3ExpressiveTokens.spatialFast()) +
                        fadeOut(animationSpec = M3ExpressiveTokens.effectsFast())
            ) {
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(M3ExpressiveTokens.CornerMedium)
                            .clickable(onClick = onConfigureAppsClick)
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sidebar_per_app_config),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.sidebar_per_app_config_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = M3ExpressiveTokens.CornerFull
                        ) {
                            Text(
                                text = stringResource(R.string.action_configure),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = mainEnabled,
                enter = expandVertically(animationSpec = M3ExpressiveTokens.spatialFast()) +
                        fadeIn(animationSpec = M3ExpressiveTokens.effectsFast()),
                exit = shrinkVertically(animationSpec = M3ExpressiveTokens.spatialFast()) +
                        fadeOut(animationSpec = M3ExpressiveTokens.effectsFast())
            ) {
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    ExpressiveSwitchRow(
                        title = stringResource(R.string.sidebar_predicted_apps),
                        summary = stringResource(R.string.sidebar_predicted_apps_summary),
                        checked = predictedAppsChecked,
                        onCheckedChange = onPredictedAppsChange
                    )
                }
            }
        }
    }
}

/**
 * Horizontal shelf displaying pinned apps mirroring the floating sidebar dock.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedAppsDockShelf(
    pinnedApps: List<SidebarAppInfo>,
    onUnpin: (SidebarAppInfo) -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = M3ExpressiveTokens.CornerLargeIncreased,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(animationSpec = M3ExpressiveTokens.spatialDefault())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.sidebar_active_dock_title),
                        style = M3ExpressiveTypography.TitleSmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.sidebar_active_dock_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val count = pinnedApps.size
                val badgeContainerColor by animateColorAsState(
                    targetValue = if (count > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    animationSpec = M3ExpressiveTokens.effectsFast(),
                    label = "DockCountBadgeContainer"
                )
                val badgeContentColor by animateColorAsState(
                    targetValue = if (count > 0) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = M3ExpressiveTokens.effectsFast(),
                    label = "DockCountBadgeContent"
                )

                Surface(
                    color = badgeContainerColor,
                    shape = M3ExpressiveTokens.CornerFull
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = count,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically(animationSpec = M3ExpressiveTokens.spatialFast()) { height -> height } +
                                     fadeIn(animationSpec = M3ExpressiveTokens.effectsFast()))
                                    .togetherWith(
                                     slideOutVertically(animationSpec = M3ExpressiveTokens.spatialFast()) { height -> -height } +
                                     fadeOut(animationSpec = M3ExpressiveTokens.effectsFast()))
                                } else {
                                    (slideInVertically(animationSpec = M3ExpressiveTokens.spatialFast()) { height -> -height } +
                                     fadeIn(animationSpec = M3ExpressiveTokens.effectsFast()))
                                    .togetherWith(
                                     slideOutVertically(animationSpec = M3ExpressiveTokens.spatialFast()) { height -> height } +
                                     fadeOut(animationSpec = M3ExpressiveTokens.effectsFast()))
                                }
                            },
                            label = "DockCountRoll"
                        ) { targetCount ->
                            Text(
                                text = "$targetCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeContentColor
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.sidebar_pinned_badge_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeContentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = pinnedApps.isEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = M3ExpressiveTokens.effectsFast())
                        .togetherWith(fadeOut(animationSpec = M3ExpressiveTokens.effectsFast()))
                },
                label = "DockContentTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.sidebar_no_apps_pinned_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pinnedApps, key = { "dock_${it.packageName}_${it.activityName}_${it.userId}" }) { app ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = M3ExpressiveTokens.CornerMedium,
                                modifier = Modifier
                                    .width(74.dp)
                                    .animateItem(placementSpec = M3ExpressiveTokens.spatialFast<IntOffset>())
                                    .clip(M3ExpressiveTokens.CornerMedium)
                                    .clickable {
                                        haptics.reject()
                                        onUnpin(app)
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                                ) {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        Image(
                                            painter = rememberDrawablePainter(app.icon),
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(M3ExpressiveTokens.CornerSmall)
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = M3ExpressiveTokens.CornerFull,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .offset(x = 4.dp, y = (-4).dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.action_unpin),
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tone-based filter chip with expressive spring feedback.
 */
@Composable
fun ExpressiveFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "FilterChipContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "FilterChipContentColor"
    )

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = M3ExpressiveTokens.CornerFull,
        modifier = modifier
            .clip(M3ExpressiveTokens.CornerFull)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = if (selected) {
                    M3ExpressiveTypography.LabelLargeEmphasized
                } else {
                    MaterialTheme.typography.labelLarge
                },
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Filled 56dp search bar with fully rounded corners.
 */
@Composable
fun M3ExpressiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String = stringResource(R.string.sidebar_search_apps_placeholder),
    modifier: Modifier = Modifier
) {
    val haptics = rememberExpressiveHaptics()
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholderText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = scaleIn(animationSpec = M3ExpressiveTokens.spatialFast(), initialScale = 0.3f) +
                        fadeIn(animationSpec = M3ExpressiveTokens.effectsFast()),
                exit = scaleOut(animationSpec = M3ExpressiveTokens.spatialFast(), targetScale = 0.3f) +
                        fadeOut(animationSpec = M3ExpressiveTokens.effectsFast())
            ) {
                IconButton(onClick = {
                    haptics.tap()
                    onQueryChange("")
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.action_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = M3ExpressiveTokens.CornerFull,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}

enum class ConnectedPosition {
    Single,
    Top,
    Middle,
    Bottom
}

fun getConnectedShape(position: ConnectedPosition): RoundedCornerShape {
    return when (position) {
        ConnectedPosition.Single -> M3ExpressiveTokens.CornerLargeIncreased
        ConnectedPosition.Top -> RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        ConnectedPosition.Middle -> M3ExpressiveTokens.CornerNone
        ConnectedPosition.Bottom -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp
        )
    }
}

/**
 * Connected list item with position-dependent corner radii.
 */
@Composable
fun SidebarAppListItem(
    appInfo: SidebarAppInfo,
    position: ConnectedPosition = ConnectedPosition.Single,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    val isPinned = appInfo.isSidebarApp
    val shape = remember(position) { getConnectedShape(position) }
    val containerColor by animateColorAsState(
        targetValue = if (isPinned) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "AppItemContainerColor"
    )

    Surface(
        color = containerColor,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .clickable {
                val newPinned = !isPinned
                haptics.toggle(newPinned)
                onCheckedChange(newPinned)
            }
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
                        style = if (isPinned) {
                            M3ExpressiveTypography.TitleSmallEmphasized
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Crossfade(
                        targetState = isPinned,
                        animationSpec = M3ExpressiveTokens.effectsFast(),
                        label = "AppItemSubtitleCrossfade"
                    ) { pinned ->
                        Text(
                            text = if (pinned) stringResource(R.string.sidebar_pinned_to_sidebar) else appInfo.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (pinned) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = isPinned,
                    onCheckedChange = { isChecked ->
                        haptics.toggle(isChecked)
                        onCheckedChange(isChecked)
                    },
                    thumbContent = if (isPinned) {
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
