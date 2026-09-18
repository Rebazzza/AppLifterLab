package com.example.lifterlab

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

const val BG_BACKGROUND = 0xFF131315.toInt()
const val BG_SURFACE_CONTAINER = 0xFF201F22.toInt()
const val BG_SURFACE_CONTAINER_LOW = 0xFF1C1B1D.toInt()
const val BG_SURFACE_CONTAINER_HIGH = 0xFF2A2A2C.toInt()
const val OUTLINE_VARIANT = 0xFF464554.toInt()
const val PRIMARY_ACCENT = 0xFF6567E3.toInt()
const val PRIMARY_CONTAINER = 0xFF8083FF.toInt()
const val SECONDARY_ACCENT = 0xFFFFB77D.toInt()
const val TERTIARY_ACCENT = 0xFF4EDEA3.toInt()
const val ERROR_ACCENT = 0xFFFFB4AB.toInt()
const val TEXT_ON_BACKGROUND = 0xFFE5E1E4.toInt()
const val TEXT_ON_SURFACE_VARIANT = 0xFFC7C4D7.toInt()

fun dp(dp: Float): Int = (dp * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
fun sp(sp: Float): Int = (sp * android.content.res.Resources.getSystem().displayMetrics.scaledDensity).toInt()

fun bgSurfaceCard(): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(BG_SURFACE_CONTAINER_LOW)
    cornerRadius = 12f
    setStroke(1, OUTLINE_VARIANT)
}

fun bgTagChip(): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(BG_SURFACE_CONTAINER_HIGH)
    cornerRadius = 16f
    setStroke(1, OUTLINE_VARIANT)
}

fun bgPrimaryButton(): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(PRIMARY_ACCENT)
    cornerRadius = 12f
}

fun bgPrimaryContainer(): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(PRIMARY_CONTAINER)
    cornerRadius = 12f
}

fun bgPercentBadge(): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(BG_SURFACE_CONTAINER_HIGH)
    cornerRadius = 6f
    setStroke(1, OUTLINE_VARIANT)
}

fun bgProfileHeader(): GradientDrawable = GradientDrawable(
    GradientDrawable.Orientation.BL_TR,
    intArrayOf(Color.parseColor("#1A6567E3"), Color.parseColor("#2A6567E3"), BG_BACKGROUND)
).apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = 0f
}

fun TextView.setAllCaps() {
    text = text.toString().uppercase()
}

fun TextView.setTextSizeSp(sizeSp: Float) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
}

fun TextView.setTypefaceMedium() {
    try {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    } catch (_: Exception) {
        typeface = Typeface.DEFAULT_BOLD
    }
}

fun View.setMargins(startDp: Int = 0, topDp: Int = 0, endDp: Int = 0, bottomDp: Int = 0) {
    val lp = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    lp.setMargins(dp(startDp.toFloat()), dp(topDp.toFloat()), dp(endDp.toFloat()), dp(bottomDp.toFloat()))
    layoutParams = lp
}

fun LinearLayout.setPaddingH(startDp: Int = 16, endDp: Int = 16) {
    setPadding(dp(startDp.toFloat()), paddingTop, dp(endDp.toFloat()), paddingBottom)
}

fun LinearLayout.addTextView(text: String, textColor: Int = TEXT_ON_BACKGROUND, textSize: Float = 14f, typefaceMedium: Boolean = false, margins: IntArray = intArrayOf(0, 0, 0, 0)): TextView {
    val tv = TextView(this.context)
    tv.text = text
    tv.setTextColor(textColor)
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
    if (typefaceMedium) tv.setTypefaceMedium()
    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val m0 = dp(margins[0].toFloat())
    val m1 = dp(margins[1].toFloat())
    val m2 = dp(margins[2].toFloat())
    val m3 = dp(margins[3].toFloat())
    lp.setMargins(m0, m1, m2, m3)
    tv.layoutParams = lp
    addView(tv)
    return tv
}

fun android.content.Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.toast(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
