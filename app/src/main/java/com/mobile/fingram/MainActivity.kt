package com.mobile.fingram

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import com.mobile.fingram.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Handle Custom Status Bar View + NavHost Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // 1. Status bar background view height
            binding.vStatusBar.layoutParams.height = systemBars.top
            binding.vStatusBar.requestLayout()

            // 2. NavHost padding (keep it below status bar)
            binding.navHostFragment.setPadding(0, systemBars.top, 0, 0)
            
            // 3. Bottom nav padding
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            
            insets
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideBottomNav = destination.id in listOf(
                R.id.splashFragment,
                R.id.onboardingFragment,
                R.id.pinLockFragment,
                R.id.addTransactionBottomSheet
            )
            binding.bottomNavigation.visibility = if (hideBottomNav) android.view.View.GONE else android.view.View.VISIBLE
        }

        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


//    override fun onBackPressedDispatcher() {
//        super.onBackPressedDispatcher()
//        // Back from Home shows exit confirmation - handled in HomeFragment
//    }
}
