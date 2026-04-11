package com.android.billreminder.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.billreminder.R
import com.android.billreminder.databinding.FragmentSplashBinding
import com.android.billreminder.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    private val viewModel: SplashViewModel by viewModels()

    override fun onInit() {
        binding.coinIcon.alpha = 0f
        binding.coinIcon.scaleX = 0f
        binding.coinIcon.scaleY = 0f
        binding.appNameEn.alpha = 0f
        binding.tagline.alpha = 0f
        binding.dot1.alpha = 0f
        binding.dot2.alpha = 0f
        binding.dot3.alpha = 0f
        binding.madeInIndia.alpha = 0f

        lifecycleScope.launch {
            runAnimations()
            delay(2800)
            val isFirstLaunch = viewModel.appPreferences.isFirstLaunch.first()
            val isLanguageSelected = viewModel.appPreferences.hasLanguageSelected()
            val pinEnabled = viewModel.appPreferences.isPinEnabled.first()
            val pinHash = viewModel.appPreferences.getPinHash()
            // Only show PIN lock if user actually set a PIN (hash exists). If switch was on but no PIN was ever set, go to home and clear the flag.
            val hasPin = pinEnabled && !pinHash.isNullOrBlank()
            if (pinEnabled && pinHash.isNullOrBlank()) {
                viewModel.clearPinEnabled()
            }
            when {
                isFirstLaunch -> findNavController().navigate(R.id.action_splash_to_onboarding)
                hasPin -> findNavController().navigate(R.id.action_splash_to_pin_lock)
                else -> findNavController().navigate(R.id.action_splash_to_home)
            }
        }
    }

    private suspend fun runAnimations() {
        val overshoot = OvershootInterpolator(2f)
        val fastOutSlowIn = android.view.animation.AnimationUtils.loadInterpolator(
            requireContext(),
            android.R.interpolator.fast_out_slow_in
        )

        ObjectAnimator.ofFloat(binding.coinIcon, "scaleX", 0f, 1.2f).apply { duration = 600; interpolator = overshoot }.start()
        ObjectAnimator.ofFloat(binding.coinIcon, "scaleY", 0f, 1.2f).apply { duration = 600; interpolator = overshoot }.start()
        ObjectAnimator.ofFloat(binding.coinIcon, "alpha", 0f, 1f).apply { duration = 600 }.start()

        delay(600)
        ObjectAnimator.ofFloat(binding.coinIcon, "scaleX", 1.2f, 1f).setDuration(180).start()
        ObjectAnimator.ofFloat(binding.coinIcon, "scaleY", 1.2f, 1f).setDuration(180).start()

        delay(100)
        binding.appNameEn.translationY = 50f * resources.displayMetrics.density
        ObjectAnimator.ofFloat(binding.appNameEn, "translationY", 50f * resources.displayMetrics.density, 0f).apply { duration = 400; interpolator = fastOutSlowIn }.start()
        ObjectAnimator.ofFloat(binding.appNameEn, "alpha", 0f, 1f).setDuration(400).start()

        delay(300)
        binding.tagline.translationY = 20f * resources.displayMetrics.density
        ObjectAnimator.ofFloat(binding.tagline, "translationY", 20f * resources.displayMetrics.density, 0f).setDuration(400).start()
        ObjectAnimator.ofFloat(binding.tagline, "alpha", 0f, 1f).setDuration(400).start()

        delay(400)
        listOf(binding.dot1, binding.dot2, binding.dot3).forEachIndexed { i, v ->
            ObjectAnimator.ofFloat(v, "scaleX", 0f, 1f).apply {
                duration = 200
                startDelay = i * 100L
                start()
            }
            ObjectAnimator.ofFloat(v, "scaleY", 0f, 1f).apply {
                duration = 200
                startDelay = i * 100L
                start()
            }
            ObjectAnimator.ofFloat(v, "alpha", 0f, 1f).apply {
                duration = 200
                startDelay = i * 100L
                start()
            }
        }

        delay(300)
        ObjectAnimator.ofFloat(binding.madeInIndia, "alpha", 0f, 1f).setDuration(300).start()
    }
}
