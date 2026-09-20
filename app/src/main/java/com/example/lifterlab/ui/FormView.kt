package com.example.lifterlab.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.lifterlab.BG_BACKGROUND
import com.example.lifterlab.ERROR_ACCENT
import com.example.lifterlab.dp
import com.example.lifterlab.setTextSizeSp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

open class FormView(context: Context) : LinearLayout(context) {

    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    protected val errorText: TextView = TextView(context).apply {
        setTextColor(ERROR_ACCENT)
        setTextSizeSp(14f)
        visibility = GONE
    }

    protected fun scrollContent(content: View) {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(BG_BACKGROUND)
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(content)
        }
        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    protected fun showError(message: String) {
        errorText.text = message
        errorText.visibility = VISIBLE
    }

    override fun onDetachedFromWindow() {
        scope.cancel()
        super.onDetachedFromWindow()
    }
}