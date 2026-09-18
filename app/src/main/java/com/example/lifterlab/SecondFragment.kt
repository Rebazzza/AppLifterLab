package com.example.lifterlab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.example.lifterlab.*
import android.content.res.ColorStateList

class SecondFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingH(dp(16f), dp(16f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        val button = Button(requireContext()).apply {
            text = "Previous"
            setAllCaps(true)
            setTextColor(BG_BACKGROUND)
            background = bgPrimaryButton()
            setPadding(dp(24f), dp(12f), dp(24f), dp(12f))
            setMargins(0, 16, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(48f))
        }
        button.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, FirstFragment())
                .commit()
        }
        root.addView(button)

        val textView = TextView(requireContext()).apply {
            text = "Lorem ipsum dolor sit amet."
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            setPadding(0, dp(16f), 0, 0)
            setMargins(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        root.addView(textView)

        return root
    }
}


