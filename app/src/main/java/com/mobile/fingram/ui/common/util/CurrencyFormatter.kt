package com.mobile.fingram.ui.common.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    @Volatile
    private var currencySymbol: String = "$"

    private val numberFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun setCurrencySymbol(symbol: String) {
        currencySymbol = symbol
    }

    fun getCurrencySymbol(): String = currencySymbol

    fun formatPaiseToRupee(paise: Long): String = formatUsdCents(paise)

    fun formatPaiseToRupeeWithoutSymbol(paise: Long): String =
        numberFormatter.format(paise / 100.0)

    fun formatRupee(amount: Double): String = formatAmount(amount)

    fun formatUsdCents(amountCents: Long): String = formatAmount(amountCents / 100.0)

    fun formatAmount(amount: Double): String {
        val absFormatted = numberFormatter.format(kotlin.math.abs(amount))
        return if (amount < 0) "-$currencySymbol$absFormatted" else "$currencySymbol$absFormatted"
    }
}
