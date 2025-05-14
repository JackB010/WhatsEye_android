package com.example.whatseye.utils

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.example.whatseye.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.internal.ViewUtils.dpToPx

class Tooltip(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tooltipText: TextView = findViewById(R.id.tvTooltip)
    private val ivPoint: ImageView = findViewById(R.id.ivPoint)

    @SuppressLint("SetTextI18n", "RestrictedApi", "DefaultLocale")
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e is BarEntry) {
            val hour = e.x.toInt()
            val arrowOffset = when (hour) {
                0 -> -25
                1 -> -13
                2 -> -1
                21 -> 1
                22 -> 13
                23 -> 25
                else -> 0
            }

            // Apply translation instead of margin
            ivPoint.translationX = dpToPx(context, arrowOffset)
            val usageMinutes = e.y.toInt()
            val hourFormatted = String.format("%02d", hour)
            val usageFormatted = String.format("%02d", Math.round(usageMinutes.toFloat()))
            tooltipText.text = "$usageFormatted min ⦿ $hourFormatted:00"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Position the tooltip above the selected bar
        return MPPointF((-width / 2).toFloat(), -height.toFloat())
    }
}
