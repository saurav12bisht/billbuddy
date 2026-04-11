package com.android.billreminder.ui.common.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    private val usCurrencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun formatPaiseToRupee(paise: Long): String = formatUsdCents(paise)

    fun formatPaiseToRupeeWithoutSymbol(paise: Long): String =
        usCurrencyFormatter.format(paise / 100.0).replace("$", "").trim()

    fun formatRupee(amount: Double): String = usCurrencyFormatter.format(amount)

    fun formatUsdCents(amountCents: Long): String = usCurrencyFormatter.format(amountCents / 100.0)
}
