package com.libremobileos.sidebar.service

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.app.SidebarApplication
import com.libremobileos.sidebar.bean.AppInfo
import com.libremobileos.sidebar.ui.sidebar.rememberExpressiveHaptics
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTokens

@Composable
fun SidebarComposeView(
    viewModel: ServiceViewModel,
    launchApp: (AppInfo) -> Unit,
    closeSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberExpressiveHaptics()
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences(SidebarApplication.CONFIG, Context.MODE_PRIVATE)

    val columnCount = sharedPrefs.getInt("sidebar_columns", 1)
    val iconSize = sharedPrefs.getInt("sidebar_icon_size", 40)
    val iconPadding = sharedPrefs.getInt("sidebar_icon_padding", 6)
    val columnSpacing = sharedPrefs.getInt("sidebar_column_spacing", 4)
    val cornerRadius = sharedPrefs.getFloat("sidebar_corner_radius", 28f)
    val backgroundTransparency = sharedPrefs.getFloat("sidebar_background_transparency", 0.85f)
    val showShadow = sharedPrefs.getBoolean("sidebar_show_shadow", true)

    val sidebarAppList by viewModel.sidebarAppListFlow.collectAsState()

    val containerShape = RoundedCornerShape(cornerRadius.dp)
    var surfaceModifier = modifier

    if (showShadow) {
        surfaceModifier = surfaceModifier.shadow(
            elevation = M3ExpressiveTokens.ElevationLevel2,
            shape = containerShape
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = backgroundTransparency),
        shape = containerShape,
        modifier = surfaceModifier
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .wrapContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpressiveHeroLauncherItem(
                appInfo = viewModel.allAppActivity,
                iconSize = iconSize,
                iconPadding = iconPadding,
                onClick = {
                    haptics.confirm()
                    launchApp(viewModel.allAppActivity)
                }
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .width((iconSize + iconPadding * 2 - 8).coerceAtLeast(32).dp)
                    .padding(vertical = 6.dp)
            )

            if (columnCount == 1) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(
                        items = sidebarAppList,
                        key = { "${it.packageName}_${it.activityName}_${it.userId}" }
                    ) { appInfo ->
                        ExpressiveAppItem(
                            appInfo = appInfo,
                            iconSize = iconSize,
                            iconPadding = iconPadding,
                            onClick = {
                                haptics.confirm()
                                launchApp(appInfo)
                            },
                            modifier = Modifier.animateItem(placementSpec = M3ExpressiveTokens.spatialFast<IntOffset>())
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(columnSpacing.dp),
                    verticalArrangement = Arrangement.spacedBy(columnSpacing.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .width((
                            (iconSize + iconPadding * 2) * columnCount +
                            columnSpacing * (columnCount - 1) + 8
                        ).dp)
                ) {
                    items(
                        items = sidebarAppList,
                        key = { "${it.packageName}_${it.activityName}_${it.userId}" }
                    ) { appInfo ->
                        ExpressiveAppItem(
                            appInfo = appInfo,
                            iconSize = iconSize,
                            iconPadding = iconPadding,
                            onClick = {
                                haptics.confirm()
                                launchApp(appInfo)
                            },
                            modifier = Modifier.animateItem(placementSpec = M3ExpressiveTokens.spatialFast<IntOffset>())
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .width((iconSize + iconPadding * 2 - 8).coerceAtLeast(32).dp)
                    .padding(vertical = 6.dp)
            )

            ExpressiveSettingsItem(
                iconSize = iconSize,
                iconPadding = iconPadding,
                onClick = {
                    haptics.contextClick()
                    viewModel.openSidebarSettings()
                    closeSidebar()
                }
            )
        }
    }
}

@Composable
private fun ExpressiveHeroLauncherItem(
    appInfo: AppInfo,
    iconSize: Int,
    iconPadding: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = M3ExpressiveTokens.spatialFast(),
        label = "HeroScale"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 20.dp,
        animationSpec = M3ExpressiveTokens.spatialFast(),
        label = "HeroCornerMorph"
    )

    val itemShape = RoundedCornerShape(animatedCornerRadius)
    val itemSize = (iconSize + iconPadding * 2).dp

    Box(
        modifier = modifier
            .size(itemSize)
            .scale(scale)
            .clip(itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = itemShape,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Widgets,
                    contentDescription = appInfo.label,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size((iconSize * 24 / 40).coerceAtLeast(24).dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveAppItem(
    appInfo: AppInfo,
    iconSize: Int,
    iconPadding: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = M3ExpressiveTokens.spatialFast(),
        label = "AppItemScale"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 16.dp,
        animationSpec = M3ExpressiveTokens.spatialFast(),
        label = "AppItemCornerMorph"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
        } else {
            Color.Transparent
        },
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "AppItemContainerColor"
    )

    val itemShape = RoundedCornerShape(animatedCornerRadius)
    val itemSize = (iconSize + iconPadding * 2).dp

    Box(
        modifier = modifier
            .size(itemSize)
            .scale(scale)
            .clip(itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = containerColor,
            shape = itemShape,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = rememberDrawablePainter(appInfo.icon),
                    contentDescription = appInfo.label,
                    modifier = Modifier
                        .size(iconSize.dp)
                        .clip(M3ExpressiveTokens.CornerSmall)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveSettingsItem(
    iconSize: Int,
    iconPadding: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = M3ExpressiveTokens.spatialFast(),
        label = "SettingsScale"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 28.dp,
        animationSpec = M3ExpressiveTokens.spatialFast(),
        label = "SettingsCornerMorph"
    )

    val itemShape = RoundedCornerShape(animatedCornerRadius)
    val itemSize = (iconSize + iconPadding * 2).dp

    Box(
        modifier = modifier
            .size(itemSize)
            .scale(scale)
            .clip(itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            shape = itemShape,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.sidebar_settings_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size((iconSize * 24 / 40).coerceAtLeast(22).dp)
                )
            }
        }
    }
}
