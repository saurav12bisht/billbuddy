package com.android.billreminder.ui.common.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView

/**
 * SwipeHintHelper
 *
 * Shows a one-time animated "swipe left to delete" hint on the first transaction
 * row the user ever sees. The hint is stored in SharedPreferences so it only
 * plays once across app sessions.
 *
 * Usage (from Fragment):
 *   SwipeHintHelper.showIfNeeded(requireContext(), binding.rvTransactions)
 *
 * Call this AFTER the adapter has submitted its first non-empty list, so the
 * RecyclerView has at least one visible row to animate.
 */
object SwipeHintHelper {

    private const val PREFS_NAME  = "bill_reminder_prefs"
    private const val KEY_SHOWN   = "swipe_hint_shown"

    // How far (px) to slide the row left during the hint
    private const val SWIPE_DISTANCE_DP = 80f

    // Total duration of one full hint cycle (slide out → pause → slide back)
    private const val SLIDE_OUT_MS  = 380L
    private const val PAUSE_MS      = 320L
    private const val SLIDE_BACK_MS = 280L

    // Delay before the hint starts, giving the enter-animations time to finish
    private const val START_DELAY_MS = 600L

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAlreadyShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOWN, false)

    private fun markShown(context: Context) =
        prefs(context).edit().putBoolean(KEY_SHOWN, true).apply()

    /**
     * Call after the list is populated. If the hint was never shown before,
     * finds the first transaction row (skips the day-header at position 0)
     * and runs the slide animation on it.
     */
    fun showIfNeeded(context: Context, recyclerView: RecyclerView) {
        if (isAlreadyShown(context)) return

        // Wait until the RecyclerView has laid out its children
        recyclerView.post {
            // Position 0 is typically a DayHeader — try position 1 first,
            // fall back to 0 if only one child is visible.
            val targetView: View? = recyclerView.getChildAt(1) ?: recyclerView.getChildAt(0)
            if (targetView == null) return@post

            markShown(context)
            playSwipeHint(targetView, recyclerView.context)
        }
    }

    private fun playSwipeHint(view: View, context: Context) {
        val density   = context.resources.displayMetrics.density
        val swipeDist = SWIPE_DISTANCE_DP * density  // convert dp → px

        // ── Phase 1: slide left + fade out ───────────────────────────────────
        val slideOut = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, -swipeDist).apply {
            duration     = SLIDE_OUT_MS
            startDelay   = START_DELAY_MS
            interpolator = DecelerateInterpolator(1.4f)
        }
        val fadeOut = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.6f).apply {
            duration   = SLIDE_OUT_MS
            startDelay = START_DELAY_MS
        }

        // ── Phase 2: slide back + fade in (after pause) ──────────────────────
        val slideBack = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, -swipeDist, 0f).apply {
            duration     = SLIDE_BACK_MS
            startDelay   = PAUSE_MS   // pause at the peak before springing back
            interpolator = AccelerateDecelerateInterpolator()
        }
        val fadeIn = ObjectAnimator.ofFloat(view, View.ALPHA, 0.6f, 1f).apply {
            duration   = SLIDE_BACK_MS
            startDelay = PAUSE_MS
        }

        // ── Sequence: phase1 then phase2 ─────────────────────────────────────
        val phase1 = AnimatorSet().apply { playTogether(slideOut, fadeOut) }
        val phase2 = AnimatorSet().apply { playTogether(slideBack, fadeIn) }

        AnimatorSet().apply {
            playSequentially(phase1, phase2)
            start()
        }
    }
}