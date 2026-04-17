package com.android.fingram.ui.onboarding

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.android.fingram.R
import com.android.fingram.databinding.FragmentOnboardingBinding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.fingram.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(FragmentOnboardingBinding::inflate) {

    private val viewModel: OnboardingViewModel by viewModels()

    override fun onInit() {
        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        TabLayoutMediator(binding.dotsTab, binding.viewPager) { _, _ -> }.attach()

        binding.btnSkip.setOnClickListener {
            lifecycleScope.launch {
                viewModel.completeOnboarding()
                findNavController().navigate(R.id.action_onboarding_to_home)
            }
        }

        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem < 2) {
                binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
            }
        }

        binding.btnGetStarted.setOnClickListener {
            lifecycleScope.launch {
                viewModel.completeOnboarding()
                findNavController().navigate(R.id.action_onboarding_to_home)
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.btnSkip.visibility = if (position < 2) android.view.View.VISIBLE else android.view.View.GONE
                binding.btnNext.visibility = if (position < 2) android.view.View.VISIBLE else android.view.View.GONE
                binding.btnGetStarted.visibility = if (position == 2) android.view.View.VISIBLE else android.view.View.GONE
            }
        })
        binding.btnGetStarted.visibility = android.view.View.GONE
    }
}
