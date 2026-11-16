/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.libremobileos.sidebar.ui.sidebar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTokens
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTypography

/**
 * Live preview stage rendering custom sidebar settings on a simulated device bezel.
 */
@Composable
fun SidebarHeroLivePreview(
    position: Int,
    columnCount: Int,
    cornerRadius: Float,
    backgroundTransparency: Float,
    showShadow: Boolean,
    sliderTransparency: Float,
    sliderLength: Int,
    sliderWidth: Int,
    iconSize: Int,
    iconPadding: Int,
    columnSpacing: Int
) {
    val previewScale = 0.32f

    val targetTileSize = ((iconSize + iconPadding * 2) * previewScale).coerceIn(14f, 44f).dp
    val animatedTileSize by animateDpAsState(
        targetValue = targetTileSize,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewTileSize"
    )

    val targetIconSize = (iconSize * previewScale).coerceIn(8f, 28f).dp
    val animatedIconSize by animateDpAsState(
        targetValue = targetIconSize,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewIconSize"
    )

    val targetPadding = (iconPadding * previewScale).coerceIn(0f, 10f).dp
    val animatedTilePadding by animateDpAsState(
        targetValue = targetPadding,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewTilePadding"
    )

    val targetColSpacing = (columnSpacing * previewScale).coerceIn(1f, 8f).dp
    val animatedColSpacing by animateDpAsState(
        targetValue = targetColSpacing,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewColSpacing"
    )

    val animatedRadius by animateDpAsState(
        targetValue = (cornerRadius * 0.40f).coerceIn(0f, 24f).dp,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewCornerRadius"
    )

    val targetGridWidth = (targetTileSize * columnCount + targetColSpacing * (columnCount - 1)).coerceAtLeast(targetTileSize)
    val animatedGridWidth by animateDpAsState(
        targetValue = targetGridWidth,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewGridWidth"
    )

    val targetWidth = targetGridWidth + 16.dp
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewWidth"
    )

    val animatedBgAlpha by animateFloatAsState(
        targetValue = backgroundTransparency,
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "PreviewBgAlpha"
    )
    val animatedTriggerAlpha by animateFloatAsState(
        targetValue = sliderTransparency,
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "PreviewTriggerAlpha"
    )
    val animatedTriggerHeight by animateDpAsState(
        targetValue = (sliderLength * 0.24f).coerceIn(28f, 100f).dp,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewTriggerHeight"
    )
    val animatedTriggerWidth by animateDpAsState(
        targetValue = (sliderWidth * 0.045f).coerceIn(3.5f, 9f).dp,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewTriggerWidth"
    )

    val targetBias = if (position == -1) -1f else 1f
    val animatedBias by animateFloatAsState(
        targetValue = targetBias,
        animationSpec = M3ExpressiveTokens.spatialDefault(),
        label = "PreviewPositionBias"
    )
    val animatedAlignment = BiasAlignment(horizontalBias = animatedBias, verticalBias = 0f)
    val isLeft = animatedBias < 0f

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = M3ExpressiveTokens.CornerExtraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.preview_title),
                    style = M3ExpressiveTypography.TitleMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = M3ExpressiveTokens.CornerFull
                ) {
                    Text(
                        text = if (position == -1) stringResource(R.string.preview_edge_left) else stringResource(R.string.preview_edge_right),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "9:41",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        )
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        )
                    }
                }

                // Camera punch-hole at left center (tablet / landscape orientation)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                )

                Box(
                    modifier = Modifier
                        .align(animatedAlignment)
                        .height(animatedTriggerHeight.coerceAtLeast(1.dp))
                        .width(animatedTriggerWidth.coerceAtLeast(1.dp))
                        .clip(
                            RoundedCornerShape(
                                topStart = if (isLeft) 0.dp else 4.dp,
                                bottomStart = if (isLeft) 0.dp else 4.dp,
                                topEnd = if (isLeft) 4.dp else 0.dp,
                                bottomEnd = if (isLeft) 4.dp else 0.dp
                            )
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = animatedTriggerAlpha.coerceIn(0.2f, 1.0f)
                            )
                        )
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(
                        alpha = animatedBgAlpha.coerceIn(0f, 1f)
                    ),
                    shape = RoundedCornerShape(animatedRadius.coerceAtLeast(0.dp)),
                    shadowElevation = if (showShadow) 6.dp else 0.dp,
                    modifier = Modifier
                        .align(animatedAlignment)
                        .padding(horizontal = 8.dp)
                        .width(animatedWidth.coerceAtLeast(32.dp))
                        .wrapContentHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape((animatedTileSize * 0.28f).coerceAtLeast(0.dp)),
                            modifier = Modifier.size(animatedTileSize.coerceAtLeast(1.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(
                                        (animatedIconSize * 0.65f)
                                            .coerceAtMost(animatedTileSize - 4.dp)
                                            .coerceAtLeast(4.dp)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(animatedGridWidth.coerceAtLeast(16.dp))
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(animatedColSpacing.coerceAtLeast(0.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (c in 1..columnCount) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(animatedColSpacing.coerceAtLeast(0.dp)),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    for (row in 1..2) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            shape = RoundedCornerShape((animatedTileSize * 0.24f).coerceAtLeast(0.dp)),
                                            modifier = Modifier.size(animatedTileSize.coerceAtLeast(1.dp))
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(
                                                        animatedTilePadding
                                                            .coerceAtLeast(0.dp)
                                                            .coerceAtMost(((animatedTileSize - 4.dp) / 2).coerceAtLeast(0.dp))
                                                    )
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape((animatedIconSize * 0.22f).coerceAtLeast(0.dp)),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(animatedGridWidth.coerceAtLeast(16.dp))
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape((animatedTileSize * 0.28f).coerceAtLeast(0.dp)),
                            modifier = Modifier.size(animatedTileSize.coerceAtLeast(1.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(
                                        (animatedIconSize * 0.65f)
                                            .coerceAtMost(animatedTileSize - 4.dp)
                                            .coerceAtLeast(4.dp)
                                    )
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .width(36.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                )
            }
        }
    }
}
