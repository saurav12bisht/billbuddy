package com.android.fingram.ui.tests

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.fingram.MainActivity
import com.android.fingram.R
import com.android.fingram.data.preferences.AppPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillReminderFlowTest {

    private lateinit var appPreferences: AppPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        appPreferences = AppPreferences(context)
        runBlocking {
            appPreferences.clear()
            appPreferences.setLanguage("en")
            appPreferences.setFirstLaunchComplete()
            appPreferences.setPinEnabled(false)
        }
    }

    @Test
    fun addBill_andNavigateAcrossPrimaryTabs() {
        ActivityScenario.launch(MainActivity::class.java)
        waitForSplash()

        onView(withId(R.id.fabAddBill)).perform(click())
        onView(withId(R.id.etAmount)).perform(replaceText("89.99"))
        closeSoftKeyboard()
        onView(withId(R.id.etBillName)).perform(replaceText("Internet Bill"))
        closeSoftKeyboard()
        onView(withId(R.id.btnSaveBill)).perform(click())

        onView(withText("Internet Bill")).check(matches(isDisplayed()))

        onView(withText("Dashboard")).perform(click())
        onView(withId(R.id.tvMonthlyTotal)).check(matches(isDisplayed()))

        onView(withText("History")).perform(click())
        onView(withId(R.id.historyChart)).check(matches(isDisplayed()))

        onView(withText("Settings")).perform(click())
        onView(withId(R.id.switchReminder7)).check(matches(isDisplayed()))

        onView(withText("Bills")).perform(click())
        onView(withText("Internet Bill")).check(matches(isDisplayed()))
    }

    private fun waitForSplash() {
        SystemClock.sleep(3500)
    }
}
