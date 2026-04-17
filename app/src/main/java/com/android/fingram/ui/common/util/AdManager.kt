package com.android.fingram.ui.common.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    const val BANNER_ID: String = "ca-app-pub-4233897371975000/8574772089"
    private const val INTERSTITIAL_ID: String = "ca-app-pub-4233897371975000/4853242820"
    private const val REWARDED_ID: String = "ca-app-pub-4233897371975000/4853242820"

    private var interstitial: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var hasShownDashboardInterstitialThisSession = false

    fun initialize(context: Context) {
        val testDeviceIds = java.util.Arrays.asList("7E58F7098B9DA1E30E96A2A4CF49B462")
        val configuration = com.google.android.gms.ads.RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.initialize(context)
        loadInterstitial(context)
        loadRewarded(context)
    }

    fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(err: LoadAdError) {
                    interstitial = null
                }
            }
        )
    }

    fun maybeShowDashboardInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        if (hasShownDashboardInterstitialThisSession || ad == null) {
            onDone()
            return
        }
        hasShownDashboardInterstitialThisSession = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial(activity)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                interstitial = null
                loadInterstitial(activity)
                onDone()
            }
        }
        ad.show(activity)
    }

    fun onTransactionSaved(activity: Activity, onDone: () -> Unit) {
        onDone()
    }

    fun showInterstitialBeforeExport(activity: Activity, onDone: () -> Unit) {
        maybeShowDashboardInterstitial(activity, onDone)
    }

    fun showRewarded(activity: Activity, onRewarded: () -> Unit, onDismissed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded(activity)
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                loadRewarded(activity)
                onDismissed()
            }
        }
        ad.show(activity) { _: RewardItem ->
            onRewarded()
        }
    }

    private fun loadRewarded(context: Context) {
        RewardedAd.load(
            context,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }
}
