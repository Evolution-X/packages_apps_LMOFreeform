/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.libremobileos.sidebar.ui.sidebar

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarCustomizationSettingsPage(
    sharedPrefs: SharedPreferences,
    onBack: () -> Unit = {},
    onSettingChanged: () -> Unit = {}
) {
    val haptics = rememberExpressiveHaptics()
    var transparency by remember { mutableStateOf(sharedPrefs.getFloat("slider_transparency", 0.80f)) }
    var sliderLength by remember { mutableStateOf(sharedPrefs.getInt("slider_length", 200)) }
    var sliderWidth by remember { mutableStateOf(sharedPrefs.getInt("slider_width", 100)) }
    var position by remember { mutableStateOf(sharedPrefs.getInt("sideline_position_x", 1)) }
    var columnCount by remember { mutableStateOf(sharedPrefs.getInt("sidebar_columns", 1)) }
    var iconSize by remember { mutableStateOf(sharedPrefs.getInt("sidebar_icon_size", 40)) }
    var iconPadding by remember { mutableStateOf(sharedPrefs.getInt("sidebar_icon_padding", 6)) }
    var columnSpacing by remember { mutableStateOf(sharedPrefs.getInt("sidebar_column_spacing", 4)) }
    var cornerRadius by remember { mutableStateOf(sharedPrefs.getFloat("sidebar_corner_radius", 28f)) }
    var backgroundTransparency by remember { mutableStateOf(sharedPrefs.getFloat("sidebar_background_transparency", 0.85f)) }
    var showShadow by remember { mutableStateOf(sharedPrefs.getBoolean("sidebar_show_shadow", true)) }
    var tapToOpen by remember { mutableStateOf(sharedPrefs.getBoolean("sidebar_tap_to_open", false)) }
    var swipeToOpen by remember { mutableStateOf(sharedPrefs.getBoolean("sidebar_swipe_to_open", true)) }
    var hideOnGameSpace by remember { mutableStateOf(sharedPrefs.getBoolean("sidebar_hide_on_gamespace", false)) }

    DisposableEffect(sharedPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "sideline_position_x" -> {
                    position = prefs.getInt("sideline_position_x", position)
                }
                "slider_length" -> {
                    sliderLength = prefs.getInt("slider_length", sliderLength)
                }
                "slider_width" -> {
                    sliderWidth = prefs.getInt("slider_width", sliderWidth)
                }
                "slider_transparency" -> {
                    transparency = prefs.getFloat("slider_transparency", transparency)
                }
                "sidebar_columns" -> {
                    columnCount = prefs.getInt("sidebar_columns", columnCount)
                }
                "sidebar_icon_size" -> {
                    iconSize = prefs.getInt("sidebar_icon_size", iconSize)
                }
                "sidebar_icon_padding" -> {
                    iconPadding = prefs.getInt("sidebar_icon_padding", iconPadding)
                }
                "sidebar_column_spacing" -> {
                    columnSpacing = prefs.getInt("sidebar_column_spacing", columnSpacing)
                }
                "sidebar_corner_radius" -> {
                    cornerRadius = prefs.getFloat("sidebar_corner_radius", cornerRadius)
                }
                "sidebar_background_transparency" -> {
                    backgroundTransparency = prefs.getFloat("sidebar_background_transparency", backgroundTransparency)
                }
                "sidebar_show_shadow" -> {
                    showShadow = prefs.getBoolean("sidebar_show_shadow", showShadow)
                }
                "sidebar_tap_to_open" -> {
                    tapToOpen = prefs.getBoolean("sidebar_tap_to_open", tapToOpen)
                }
                "sidebar_swipe_to_open" -> {
                    swipeToOpen = prefs.getBoolean("sidebar_swipe_to_open", swipeToOpen)
                }
                "sidebar_hide_on_gamespace" -> {
                    hideOnGameSpace = prefs.getBoolean("sidebar_hide_on_gamespace", hideOnGameSpace)
                }
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    CompositionLocalProvider(LocalNavController provides remember {
        object : NavControllerWrapper {
            override fun navigate(route: String, popUpCurrent: Boolean) {}
            override fun navigateBack() { onBack() }
        }
    }) {
        SettingsScaffold(
            title = stringResource(R.string.sidebar_customization_title)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SidebarHeroLivePreview(
                    position = position,
                    columnCount = columnCount,
                    cornerRadius = cornerRadius,
                    backgroundTransparency = backgroundTransparency,
                    showShadow = showShadow,
                    sliderTransparency = transparency,
                    sliderLength = sliderLength,
                    sliderWidth = sliderWidth,
                    iconSize = iconSize,
                    iconPadding = iconPadding,
                    columnSpacing = columnSpacing
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = M3ExpressiveTokens.CornerLargeIncreased,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeaderWithBadge(
                                title = stringResource(R.string.sidebar_section_layout),
                                badgeLabel = stringResource(R.string.badge_layout),
                                badgeColor = MaterialTheme.colorScheme.primaryContainer,
                                badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Text(
                                text = pluralStringResource(
                                    R.plurals.sidebar_columns_value, columnCount, columnCount
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val col1Shape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    bottomStart = 20.dp,
                                    topEnd = 4.dp,
                                    bottomEnd = 4.dp
                                )
                                val col2Shape = RoundedCornerShape(4.dp)
                                val col3Shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    bottomStart = 4.dp,
                                    topEnd = 20.dp,
                                    bottomEnd = 20.dp
                                )

                                for (i in 1..3) {
                                    val shape = when (i) {
                                        1 -> col1Shape
                                        2 -> col2Shape
                                        else -> col3Shape
                                    }
                                    ExpressiveConnectedButton(
                                        text = pluralStringResource(R.plurals.sidebar_columns_value, i, i),
                                        isSelected = columnCount == i,
                                        shape = shape,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            columnCount = i
                                            sharedPrefs.edit().putInt("sidebar_columns", columnCount).apply()
                                            onSettingChanged()
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            ExpressiveSliderWithBadge(
                                title = stringResource(R.string.sidebar_corner_radius_value, cornerRadius.toInt()),
                                badgeText = stringResource(R.string.unit_dp, cornerRadius.toInt()),
                                value = cornerRadius,
                                onValueChange = {
                                    cornerRadius = it
                                    sharedPrefs.edit().putFloat("sidebar_corner_radius", cornerRadius).apply()
                                    onSettingChanged()
                                },
                                valueRange = 0f..48f,
                                steps = 11
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ExpressiveSliderWithBadge(
                                title = stringResource(R.string.sidebar_bg_transparency_value, backgroundTransparency),
                                badgeText = stringResource(R.string.unit_percent, (backgroundTransparency * 100).toInt()),
                                value = backgroundTransparency,
                                onValueChange = {
                                    backgroundTransparency = it
                                    sharedPrefs.edit().putFloat("sidebar_background_transparency", backgroundTransparency).apply()
                                    onSettingChanged()
                                },
                                valueRange = 0.1f..1.0f,
                                steps = 8
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ExpressiveSliderWithBadge(
                                title = stringResource(R.string.sidebar_icon_size_value, iconSize),
                                badgeText = stringResource(R.string.unit_dp, iconSize),
                                value = iconSize.toFloat(),
                                onValueChange = {
                                    iconSize = it.toInt()
                                    sharedPrefs.edit().putInt("sidebar_icon_size", iconSize).apply()
                                    onSettingChanged()
                                },
                                valueRange = 30f..80f,
                                steps = 9
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ExpressiveSliderWithBadge(
                                title = stringResource(R.string.sidebar_icon_padding_value, iconPadding),
                                summary = stringResource(R.string.sidebar_icon_padding_summary),
                                badgeText = stringResource(R.string.unit_dp, iconPadding),
                                value = iconPadding.toFloat(),
                                onValueChange = {
                                    iconPadding = it.toInt()
                                    sharedPrefs.edit().putInt("sidebar_icon_padding", iconPadding).apply()
                                    onSettingChanged()
                                },
                                valueRange = 4f..20f,
                                steps = 15
                            )

                            if (columnCount > 1) {
                                Spacer(modifier = Modifier.height(16.dp))
                                ExpressiveSliderWithBadge(
                                    title = stringResource(R.string.sidebar_column_spacing_value, columnSpacing),
                                    summary = stringResource(R.string.sidebar_column_spacing_summary),
                                    badgeText = stringResource(R.string.unit_dp, columnSpacing),
                                    value = columnSpacing.toFloat(),
                                    onValueChange = {
                                        columnSpacing = it.toInt()
                                        sharedPrefs.edit().putInt("sidebar_column_spacing", columnSpacing).apply()
                                        onSettingChanged()
                                    },
                                    valueRange = 1f..12f,
                                    steps = 10
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = M3ExpressiveTokens.CornerLargeIncreased,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeaderWithBadge(
                                title = stringResource(R.string.sidebar_section_slider),
                                badgeLabel = stringResource(R.string.badge_handle),
                                badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                                badgeContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Text(
                                text = stringResource(R.string.sidebar_position_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val leftShape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    bottomStart = 20.dp,
                                    topEnd = 4.dp,
                                    bottomEnd = 4.dp
                                )
                                val rightShape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    bottomStart = 4.dp,
                                    topEnd = 20.dp,
                                    bottomEnd = 20.dp
                                )

                                ExpressiveConnectedButton(
                                    text = stringResource(R.string.sidebar_left),
                                    isSelected = position == -1,
                                    shape = leftShape,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        position = -1
                                        sharedPrefs.edit().putInt("sideline_position_x", position).apply()
                                        onSettingChanged()
                                    }
                                )

                                ExpressiveConnectedButton(
                                    text = stringResource(R.string.sidebar_right),
                                    isSelected = position == 1,
                                    shape = rightShape,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        position = 1
                                        sharedPrefs.edit().putInt("sideline_position_x", position).apply()
                                        onSettingChanged()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            ExpressiveSliderWithBadge(
                                title = stringResource(R.string.sidebar_transparency_value, transparency),
                                badgeText = stringResource(R.string.unit_percent, (transparency * 100).toInt()),
                                value = transparency,
                                onValueChange = {
                                    transparency = it
                                    sharedPrefs.edit().putFloat("slider_transparency", transparency).apply()
                                    onSettingChanged()
                                },
                                valueRange = 0.1f..1.0f,
                                steps = 8
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ExpressiveSliderWithBadge(
                                title = stringResource(R.string.sidebar_length_value, sliderLength),
                                badgeText = stringResource(R.string.unit_px, sliderLength),
                                value = sliderLength.toFloat(),
                                onValueChange = {
                                    sliderLength = it.toInt()
                                    sharedPrefs.edit().putInt("slider_length", sliderLength).apply()
                                    onSettingChanged()
                                },
                                valueRange = 100f..500f,
                                steps = 19
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ExpressiveSliderWithBadge(
                                title = stringResource(R.string.sidebar_width_value, sliderWidth),
                                badgeText = stringResource(R.string.unit_px, sliderWidth),
                                value = sliderWidth.toFloat(),
                                onValueChange = {
                                    sliderWidth = it.toInt()
                                    sharedPrefs.edit().putInt("slider_width", sliderWidth).apply()
                                    onSettingChanged()
                                },
                                valueRange = 20f..200f,
                                steps = 17
                            )
                        }
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = M3ExpressiveTokens.CornerLargeIncreased,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeaderWithBadge(
                                title = stringResource(R.string.sidebar_section_visual),
                                badgeLabel = stringResource(R.string.badge_effects),
                                badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                                badgeContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )

                            ExpressiveSwitchRow(
                                title = stringResource(R.string.sidebar_drop_shadow_title),
                                summary = stringResource(R.string.sidebar_drop_shadow_summary),
                                checked = showShadow,
                                onCheckedChange = {
                                    showShadow = it
                                    sharedPrefs.edit().putBoolean("sidebar_show_shadow", showShadow).apply()
                                    onSettingChanged()
                                }
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            ExpressiveSwitchRow(
                                title = stringResource(R.string.sidebar_tap_to_open_title),
                                summary = stringResource(R.string.sidebar_tap_to_open_summary),
                                checked = tapToOpen,
                                onCheckedChange = {
                                    tapToOpen = it
                                    sharedPrefs.edit().putBoolean("sidebar_tap_to_open", tapToOpen).apply()
                                    onSettingChanged()
                                }
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            ExpressiveSwitchRow(
                                title = stringResource(R.string.sidebar_swipe_to_open_title),
                                summary = stringResource(R.string.sidebar_swipe_to_open_summary),
                                checked = swipeToOpen,
                                onCheckedChange = {
                                    swipeToOpen = it
                                    sharedPrefs.edit().putBoolean("sidebar_swipe_to_open", swipeToOpen).apply()
                                    onSettingChanged()
                                }
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            ExpressiveSwitchRow(
                                title = stringResource(R.string.sidebar_hide_on_gamespace_title),
                                summary = stringResource(R.string.sidebar_hide_on_gamespace_summary),
                                checked = hideOnGameSpace,
                                onCheckedChange = {
                                    hideOnGameSpace = it
                                    sharedPrefs.edit().putBoolean("sidebar_hide_on_gamespace", hideOnGameSpace).apply()
                                    onSettingChanged()
                                }
                            )
                        }
                    }
                }

                item {
                    FilledTonalButton(
                        onClick = {
                            haptics.confirm()
                            transparency = 0.80f
                            sliderLength = 200
                            sliderWidth = 100
                            position = 1
                            columnCount = 1
                            iconSize = 40
                            iconPadding = 6
                            columnSpacing = 4
                            cornerRadius = 28f
                            backgroundTransparency = 0.85f
                            showShadow = true
                            tapToOpen = false
                            swipeToOpen = true
                            hideOnGameSpace = false

                            sharedPrefs.edit()
                                .putFloat("slider_transparency", transparency)
                                .putInt("slider_length", sliderLength)
                                .putInt("slider_width", sliderWidth)
                                .putInt("sideline_position_x", position)
                                .putInt("sidebar_columns", columnCount)
                                .putInt("sidebar_icon_size", iconSize)
                                .putInt("sidebar_icon_padding", iconPadding)
                                .putInt("sidebar_column_spacing", columnSpacing)
                                .putFloat("sidebar_corner_radius", cornerRadius)
                                .putFloat("sidebar_background_transparency", backgroundTransparency)
                                .putBoolean("sidebar_show_shadow", showShadow)
                                .putBoolean("sidebar_tap_to_open", tapToOpen)
                                .putBoolean("sidebar_swipe_to_open", swipeToOpen)
                                .putBoolean("sidebar_hide_on_gamespace", hideOnGameSpace)
                                .apply()
                            onSettingChanged()
                        },
                        shape = M3ExpressiveTokens.CornerFull,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 32.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sidebar_reset_defaults),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
}
