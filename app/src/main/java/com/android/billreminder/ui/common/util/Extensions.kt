package com.android.billreminder.ui.common.util

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.DimenRes
import androidx.core.view.isVisible

fun View.visible() { isVisible = true }
fun View.gone() { isVisible = false }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.dp(dp: Int): Int = (context.resources.displayMetrics.density * dp).toInt()
