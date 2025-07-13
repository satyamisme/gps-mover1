package com.hamham.gpsmover.utils.ext

import android.view.HapticFeedbackConstants
import android.view.View

fun View.performHapticClick() {
    this.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
} 