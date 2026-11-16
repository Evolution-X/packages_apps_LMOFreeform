/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.libremobileos.sidebar.ui.sidebar

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTokens
import com.libremobileos.sidebar.ui.theme.M3ExpressiveTypography

@Composable
fun SectionHeaderWithBadge(
    title: String,
    badgeLabel: String,
    badgeColor: Color,
    badgeContentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = M3ExpressiveTypography.TitleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            color = badgeColor,
            shape = M3ExpressiveTokens.CornerFull
        ) {
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeContentColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun ExpressiveSliderWithBadge(
    title: String,
    badgeText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    summary: String? = null
) {
    val haptics = rememberExpressiveHaptics()
    var lastHapticValue by remember { mutableStateOf(value) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = M3ExpressiveTokens.CornerFull
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Slider(
            value = value,
            onValueChange = { newValue ->
                if (newValue != lastHapticValue) {
                    lastHapticValue = newValue
                    haptics.segmentTick()
                }
                onValueChange(newValue)
            },
            onValueChangeFinished = {
                haptics.gestureEnd()
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ExpressiveConnectedButton(
    text: String,
    isSelected: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "ConnectedButtonContainer"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = M3ExpressiveTokens.effectsFast(),
        label = "ConnectedButtonContent"
    )

    Surface(
        color = containerColor,
        shape = shape,
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .clickable {
                if (!isSelected) {
                    haptics.segmentTick()
                }
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = if (isSelected) {
                    M3ExpressiveTypography.LabelLargeEmphasized
                } else {
                    MaterialTheme.typography.labelLarge
                },
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun ExpressiveSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(M3ExpressiveTokens.CornerMedium)
            .clickable {
                val newChecked = !checked
                haptics.toggle(newChecked)
                onCheckedChange(newChecked)
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary,
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
            }
        )
    }
}
