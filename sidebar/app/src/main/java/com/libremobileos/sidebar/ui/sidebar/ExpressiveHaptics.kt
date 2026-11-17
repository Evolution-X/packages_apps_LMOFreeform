/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.libremobileos.sidebar.ui.sidebar

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Expressive Haptics:
 * Maps Android 14 native haptic feedback constants to semantic Material 3 interactions.
 */
class ExpressiveHaptics(private val view: View) {
    fun toggle(checked: Boolean) {
        val constant = if (checked) {
            HapticFeedbackConstants.TOGGLE_ON
        } else {
            HapticFeedbackConstants.TOGGLE_OFF
        }
        view.performHapticFeedback(constant)
    }

    fun confirm() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun reject() {
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    fun segmentTick() {
        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
    }

    fun segmentFrequentTick() {
        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    }

    fun gestureStart() {
        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
    }

    fun gestureEnd() {
        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
    }

    fun contextClick() {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun tap() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

@Composable
fun rememberExpressiveHaptics(): ExpressiveHaptics {
    val view = LocalView.current
    return remember(view) { ExpressiveHaptics(view) }
}
