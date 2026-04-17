package com.android.fingram.ui.common.util

import android.view.View
import android.view.animation.DecelerateInterpolator

object ListAnimations {
    fun animateItem(view: View, position: Int) {
        view.translationY = 100f
        view.alpha = 0f
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setStartDelay((position * 40L))
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}
