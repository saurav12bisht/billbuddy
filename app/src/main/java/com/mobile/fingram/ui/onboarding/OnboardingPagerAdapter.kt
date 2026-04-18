package com.mobile.fingram.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.fingram.R

class OnboardingPagerAdapter(
    private val fragment: androidx.fragment.app.Fragment
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    private val pages = listOf(
        Triple("Track Every Bill", "", "Manage all your monthly bills and payments in one place."),
        Triple("Stay on Top of Due Dates", "", "See exactly what is due in the next 7 days at a glance."),
        Triple("Smart Reminders", "", "Never miss a payment with automated reminders before due dates.")
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding_page, parent, false)
        return PageViewHolder(v)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val (titleEn, titleHi, subtitle) = pages[position]
        holder.titleEn.text = titleEn
        holder.titleHi.text = titleHi
        holder.subtitle.text = subtitle
        if (titleHi.isBlank()) {
            holder.titleHi.visibility = View.GONE
        }
    }

    override fun getItemCount() = pages.size

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleEn: TextView = itemView.findViewById(R.id.titleEn)
        val titleHi: TextView = itemView.findViewById(R.id.titleHi)
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
    }
}
