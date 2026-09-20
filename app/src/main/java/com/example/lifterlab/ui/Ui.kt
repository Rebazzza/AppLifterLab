package com.example.lifterlab

<<<<<<< HEAD
import android.content.res.ColorStateList
import android.content.res.Resources
=======
>>>>>>> 8f4969e823aa71bab509391fe5a93bdc2031f543
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
fun dp(dp: Int): Int = (dp * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
fun sp(sp: Float): Int = (sp * android.content.res.Resources.getSystem().displayMetrics.scaledDensity).toInt()
fun sp(sp: Int): Int = (sp * android.content.res.Resources.getSystem().displayMetrics.scaledDensity).toInt()

fun bgSurfaceCard(): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(BG_SURFACE_CONTAINER_LOW)
    cornerRadius = 12f
    setStroke(1, OUTLINE_VARIANT)
}

fun bgTextField(): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(BG_SURFACE_CONTAINER_HIGH)
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

fun android.content.Context.fieldLabel(text: String): TextView = TextView(this).apply {
    this.text = text
    setTextColor(TEXT_ON_SURFACE_VARIANT)
    setTextSizeSp(13f)
    setTypefaceMedium()
}

fun android.content.Context.textField(
    hint: String,
    inputType: Int = InputType.TYPE_CLASS_TEXT
): EditText = EditText(this).apply {
    this.hint = hint
    setTextColor(TEXT_ON_BACKGROUND)
    setHintTextColor(TEXT_ON_SURFACE_VARIANT)
    setInputType(inputType)
    textSize = 16f
    background = bgTextField()
    setPadding(dp(16), dp(12), dp(16), dp(12))
    setSingleLine(true)
}

fun android.content.Context.passwordToggleField(hint: String): TextInputLayout {
    val editText = TextInputEditText(this).apply {
        setTextColor(TEXT_ON_BACKGROUND)
        setHintTextColor(TEXT_ON_SURFACE_VARIANT)
        setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        textSize = 16f
        setSingleLine(true)
    }
    return TextInputLayout(this).apply {
        this.hint = hint
        addView(editText)
        endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        setEndIconTintList(ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT))
        boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        setBoxStrokeColorStateList(ColorStateList.valueOf(OUTLINE_VARIANT))
        setBoxBackgroundColorStateList(ColorStateList.valueOf(BG_SURFACE_CONTAINER_HIGH))
        boxStrokeWidth = dp(1)
        shapeAppearanceModel = shapeAppearanceModel.toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, dp(12).toFloat())
            .build()
        setHintTextColor(ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT))
    }
}

fun android.content.Context.primaryButton(text: String): MaterialButton = MaterialButton(this).apply {
    this.text = text
    textSize = 16f
    isAllCaps = false
    backgroundTintList = ColorStateList.valueOf(PRIMARY_ACCENT)
    cornerRadius = dp(12)
    setTextColor(Color.WHITE)
}

fun android.content.Context.secondaryButton(text: String): MaterialButton = MaterialButton(this).apply {
    this.text = text
    textSize = 15f
    isAllCaps = false
    backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
    strokeColor = ColorStateList.valueOf(OUTLINE_VARIANT)
    strokeWidth = dp(1)
    cornerRadius = dp(12)
    setTextColor(TEXT_ON_SURFACE_VARIANT)
}

fun android.content.Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.toast(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
