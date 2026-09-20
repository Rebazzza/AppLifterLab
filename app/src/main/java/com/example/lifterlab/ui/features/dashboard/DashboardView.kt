package com.example.lifterlab.ui.features.dashboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.lifterlab.BG_BACKGROUND
import com.example.lifterlab.BG_SURFACE_CONTAINER_HIGH
import com.example.lifterlab.BG_SURFACE_CONTAINER_LOW
import com.example.lifterlab.OUTLINE_VARIANT
import com.example.lifterlab.PRIMARY_ACCENT
import com.example.lifterlab.PRIMARY_CONTAINER
import com.example.lifterlab.TERTIARY_ACCENT
import com.example.lifterlab.TEXT_ON_BACKGROUND
import com.example.lifterlab.TEXT_ON_SURFACE_VARIANT
import com.example.lifterlab.bgSurfaceCard
import com.example.lifterlab.bgTagChip
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.dp
import com.example.lifterlab.secondaryButton
import com.example.lifterlab.setMargins
import com.example.lifterlab.setTextSizeSp
import com.example.lifterlab.setTypefaceMedium
import com.example.lifterlab.ui.FormView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DashboardView(
    context: Context,
    private val onNavigateToRoutines: () -> Unit,
    private val onNavigateToProfile: () -> Unit,
    private val authRepository: AuthRepository = AuthRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : FormView(context) {

    private val userId: String = authRepository.getCurrentUser()?.uid.orEmpty()
    private var profile: UserProfile = UserProfile()

    // Elementos de la UI
    private val cycleText: TextView = TextView(context).apply {
        text = "CURRENT CYCLE: WEEK 4 / PEAK"
        setTextColor(TEXT_ON_SURFACE_VARIANT)
        setTextSizeSp(12f)
        typeface = Typeface.MONOSPACE
        setMargins(topDp = 4)
    }

    // Squat Card Views
    private val squatWeightText: TextView = statTextView("185")
    private val squatGoalText: TextView = goalTextView("85% to 200kg Goal")
    private val squatProgressBar: View = createProgressBar(0.85f)

    // Bench Card Views
    private val benchWeightText: TextView = statTextView("120")
    private val benchGoalText: TextView = goalTextView("90% to 130kg Goal")
    private val benchProgressBar: View = createProgressBar(0.90f)

    // Deadlift Card Views
    private val deadliftWeightText: TextView = statTextView("225")
    private val deadliftGoalText: TextView = goalTextView("95% to 235kg Goal")
    private val deadliftProgressBar: View = createProgressBar(0.95f)

    // Streak View
    private val streakCountText: TextView = TextView(context).apply {
        text = "4"
        setTextColor(TEXT_ON_BACKGROUND)
        setTextSizeSp(32f)
        setTypefaceMedium()
        typeface = Typeface.MONOSPACE
    }

    // Last Session Views
    private val volumeText: TextView = TextView(context).apply {
        text = "12,450 kg"
        setTextColor(TEXT_ON_BACKGROUND)
        setTextSizeSp(22f)
        setTypefaceMedium()
        typeface = Typeface.MONOSPACE
    }

    private val durationText: TextView = TextView(context).apply {
        text = "94 min"
        setTextColor(TEXT_ON_BACKGROUND)
        setTextSizeSp(22f)
        setTypefaceMedium()
        typeface = Typeface.MONOSPACE
    }

    private val content: LinearLayout = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(20), dp(36), dp(20), dp(32))

        // Top App Bar
        val topBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val titleBox = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

                addView(TextView(context).apply {
                    text = "Dashboard"
                    setTextColor(TEXT_ON_BACKGROUND)
                    setTextSizeSp(28f)
                    setTypefaceMedium()
                })
                addView(cycleText)
            }
            addView(titleBox)

            // Avatar / Profile icon
            val avatarIcon = TextView(context).apply {
                text = "👤"
                setTextSizeSp(20f)
                gravity = Gravity.CENTER
                background = bgTagChip(BG_SURFACE_CONTAINER_HIGH, OUTLINE_VARIANT)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener { onNavigateToProfile() }
            }
            addView(avatarIcon)
        }
        addView(topBar)

        // 1. SBD Metric Cards
        addView(createSbdCard("Squat", "↑ +2.5KG", squatWeightText, squatProgressBar, squatGoalText))
        addView(createSbdCard("Bench", "-", benchWeightText, benchProgressBar, benchGoalText, isNeutral = true))
        addView(createSbdCard("Deadlift", "↑ +5.0KG", deadliftWeightText, deadliftProgressBar, deadliftGoalText))

        // 2. Consistency Card (Training Frequency)
        addView(createConsistencyCard())

        // 3. Last Session Card
        addView(createLastSessionCard())

        // 4. Alert Banner
        addView(createAlertBanner())

        // 5. Bottom Navigation Bar
        addView(createBottomNavBar())
    }

    init {
        scrollContent(content)
        loadProfileData()
    }

    private fun statTextView(initialVal: String): TextView = TextView(context).apply {
        text = initialVal
        setTextColor(TEXT_ON_BACKGROUND)
        setTextSizeSp(38f)
        setTypefaceMedium()
        typeface = Typeface.MONOSPACE
    }

    private fun goalTextView(initialVal: String): TextView = TextView(context).apply {
        text = initialVal
        setTextColor(TEXT_ON_SURFACE_VARIANT)
        setTextSizeSp(12f)
        typeface = Typeface.MONOSPACE
        gravity = Gravity.END
        setMargins(topDp = 8)
    }

    private fun createProgressBar(fraction: Float): View {
        val track = LinearLayout(context).apply {
            orientation = HORIZONTAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(BG_SURFACE_CONTAINER_HIGH)
                cornerRadius = dp(4).toFloat()
            }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(8))
            setMargins(topDp = 12)
        }

        val fill = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(PRIMARY_CONTAINER)
                cornerRadius = dp(4).toFloat()
            }
            val fillWidthPercent = fraction.coerceIn(0f, 1f)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, fillWidthPercent)
        }

        val emptySpace = View(context).apply {
            val emptyPercent = (1f - fraction).coerceIn(0f, 1f)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, emptyPercent)
        }

        track.addView(fill)
        track.addView(emptySpace)
        return track
    }

    private fun createSbdCard(
        title: String,
        badgeText: String,
        weightTextView: TextView,
        progressBarView: View,
        goalTextView: TextView,
        isNeutral: Boolean = false
    ): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setMargins(topDp = 16)
        }

        // Header
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(TextView(context).apply {
                text = title
                setTextColor(TEXT_ON_BACKGROUND)
                setTextSizeSp(20f)
                setTypefaceMedium()
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            })

            val badgeColor = if (isNeutral) BG_SURFACE_CONTAINER_HIGH else TERTIARY_ACCENT
            val badgeTextColor = if (isNeutral) TEXT_ON_SURFACE_VARIANT else Color.parseColor("#003824")
            val badge = TextView(context).apply {
                text = badgeText
                setTextColor(badgeTextColor)
                setTextSizeSp(11f)
                setTypefaceMedium()
                typeface = Typeface.MONOSPACE
                background = bgTagChip(badgeColor, badgeColor)
                setPadding(dp(10), dp(5), dp(10), dp(5))
            }
            addView(badge)
        }
        card.addView(header)

        // Weight Row
        val weightRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
            setMargins(topDp = 8)

            addView(weightTextView)

            addView(TextView(context).apply {
                text = " KG"
                setTextColor(TEXT_ON_SURFACE_VARIANT)
                setTextSizeSp(14f)
                setTypefaceMedium()
                typeface = Typeface.MONOSPACE
                setPadding(dp(4), 0, 0, dp(6))
            })
        }
        card.addView(weightRow)

        // Progress Bar & Goal Text
        card.addView(progressBarView)
        card.addView(goalTextView)

        return card
    }

    private fun createConsistencyCard(): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setMargins(topDp = 16)
        }

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val titleBox = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

                addView(TextView(context).apply {
                    text = "Consistency"
                    setTextColor(TEXT_ON_BACKGROUND)
                    setTextSizeSp(20f)
                    setTypefaceMedium()
                })

                addView(TextView(context).apply {
                    text = "Training Frequency (Last 14 Days)"
                    setTextColor(TEXT_ON_SURFACE_VARIANT)
                    setTextSizeSp(12f)
                })
            }
            addView(titleBox)

            val streakBox = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                addView(streakCountText)
                addView(TextView(context).apply {
                    text = " DAY\nSTREAK"
                    setTextColor(TEXT_ON_SURFACE_VARIANT)
                    setTextSizeSp(10f)
                    setTypefaceMedium()
                    typeface = Typeface.MONOSPACE
                    setPadding(dp(4), 0, 0, 0)
                })
            }
            addView(streakBox)
        }
        card.addView(header)

        // 14 Days Grid (2 rows of 7 days)
        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        val activeDaysRow1 = listOf(false, true, true, false, true, false, true)
        val activeDaysRow2 = listOf(false, true, true, false, true, false, true)

        fun createDayRow(activeFlags: List<Boolean>): LinearLayout {
            return LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                setMargins(topDp = 10)

                for (i in 0 until 7) {
                    val isActive = activeFlags.getOrElse(i) { false }
                    val dayBox = TextView(context).apply {
                        text = dayLabels[i]
                        setTextColor(if (isActive) PRIMARY_CONTAINER else TEXT_ON_SURFACE_VARIANT)
                        setTextSizeSp(12f)
                        setTypefaceMedium()
                        typeface = Typeface.MONOSPACE
                        gravity = Gravity.CENTER
                        background = bgTagChip(
                            if (isActive) BG_SURFACE_CONTAINER_HIGH else BG_SURFACE_CONTAINER_LOW,
                            if (isActive) PRIMARY_ACCENT else OUTLINE_VARIANT
                        )
                        layoutParams = LayoutParams(0, dp(38), 1f)
                    }
                    addView(dayBox)
                    if (i < 6) dayBox.setMargins(endDp = 6)
                }
            }
        }

        card.addView(createDayRow(activeDaysRow1))
        card.addView(createDayRow(activeDaysRow2))

        return card
    }

    private fun createLastSessionCard(): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setMargins(topDp = 16)
        }

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(TextView(context).apply {
                text = "Last Session"
                setTextColor(TEXT_ON_BACKGROUND)
                setTextSizeSp(20f)
                setTypefaceMedium()
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(context).apply {
                text = "Yesterday"
                setTextColor(TEXT_ON_SURFACE_VARIANT)
                setTextSizeSp(13f)
                typeface = Typeface.MONOSPACE
            })
        }
        card.addView(header)

        // Stat 1: Total Volume
        val volumeBox = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bgTagChip(BG_SURFACE_CONTAINER_HIGH, OUTLINE_VARIANT)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setMargins(topDp = 12)

            addView(TextView(context).apply {
                text = "🏋"
                setTextSizeSp(22f)
                setPadding(0, 0, dp(12), 0)
            })

            val textWrapper = LinearLayout(context).apply {
                orientation = VERTICAL
                addView(TextView(context).apply {
                    text = "TOTAL VOLUME"
                    setTextColor(TEXT_ON_SURFACE_VARIANT)
                    setTextSizeSp(11f)
                    setTypefaceMedium()
                })
                addView(volumeText)
            }
            addView(textWrapper)
        }
        card.addView(volumeBox)

        // Stat 2: Duration
        val durationBox = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bgTagChip(BG_SURFACE_CONTAINER_HIGH, OUTLINE_VARIANT)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setMargins(topDp = 8)

            addView(TextView(context).apply {
                text = "⏱"
                setTextSizeSp(22f)
                setPadding(0, 0, dp(12), 0)
            })

            val textWrapper = LinearLayout(context).apply {
                orientation = VERTICAL
                addView(TextView(context).apply {
                    text = "DURATION"
                    setTextColor(TEXT_ON_SURFACE_VARIANT)
                    setTextSizeSp(11f)
                    setTypefaceMedium()
                })
                addView(durationText)
            }
            addView(textWrapper)
        }
        card.addView(durationBox)

        // View Log Button
        val viewLogBtn = context.secondaryButton("VIEW LOG →").apply {
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setOnClickListener { onNavigateToRoutines() }
        }
        card.addView(
            viewLogBtn,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(14) }
        )

        return card
    }

    private fun createAlertBanner(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bgTagChip(BG_SURFACE_CONTAINER_LOW, PRIMARY_ACCENT)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setMargins(topDp = 16)

            addView(TextView(context).apply {
                text = "⚠️"
                setTextSizeSp(20f)
                setPadding(0, 0, dp(12), 0)
            })

            val textWrapper = LinearLayout(context).apply {
                orientation = VERTICAL
                addView(TextView(context).apply {
                    text = "Heavy Squat Day Tomorrow"
                    setTextColor(TEXT_ON_BACKGROUND)
                    setTextSizeSp(15f)
                    setTypefaceMedium()
                })
                addView(TextView(context).apply {
                    text = "Scheduled RPE 9 singles. Prioritize sleep & recovery."
                    setTextColor(TEXT_ON_SURFACE_VARIANT)
                    setTextSizeSp(12f)
                })
            }
            addView(textWrapper)
        }
    }

    private fun createBottomNavBar(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            background = bgSurfaceCard()
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setMargins(topDp = 20)

            fun createNavItem(label: String, icon: String, isActive: Boolean, onClick: () -> Unit) = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(4), dp(6), dp(4), dp(6))
                setOnClickListener { onClick() }

                addView(TextView(context).apply {
                    text = icon
                    setTextSizeSp(18f)
                })
                addView(TextView(context).apply {
                    text = label
                    setTextColor(if (isActive) PRIMARY_ACCENT else TEXT_ON_SURFACE_VARIANT)
                    setTextSizeSp(10f)
                    setTypefaceMedium()
                    typeface = Typeface.MONOSPACE
                    setMargins(topDp = 2)
                })
            }

            addView(createNavItem("Home", "🏠", true) { /* Home activo */ })
            addView(createNavItem("Routines", "🏋", false) { onNavigateToRoutines() })
            addView(createNavItem("Tools", "📐", false) { onNavigateToRoutines() })
            addView(createNavItem("History", "📜", false) { onNavigateToRoutines() })
            addView(createNavItem("Profile", "👤", false) { onNavigateToProfile() })
        }
    }

    private fun loadProfileData() {
        if (userId.isEmpty()) return

        scope.launch {
            val result = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
            result.onSuccess { p ->
                profile = p
                renderData()
            }.onFailure {
                renderData()
            }
        }
    }

    private fun renderData() {
        squatWeightText.text = formatKg(profile.squat1RM)
        squatGoalText.text = calculateGoalPercent(profile.squat1RM, profile.squatGoalKg)

        benchWeightText.text = formatKg(profile.bench1RM)
        benchGoalText.text = calculateGoalPercent(profile.bench1RM, profile.benchGoalKg)

        deadliftWeightText.text = formatKg(profile.deadlift1RM)
        deadliftGoalText.text = calculateGoalPercent(profile.deadlift1RM, profile.deadliftGoalKg)

        streakCountText.text = profile.streakDays.toString()
        cycleText.text = "CURRENT CYCLE: WEEK ${profile.currentCycleWeek} / ${profile.cyclePhase}"
    }

    private fun formatKg(valKg: Double): String {
        return if (valKg % 1.0 == 0.0) {
            valKg.toInt().toString()
        } else {
            String.format(Locale.ROOT, "%.1f", valKg)
        }
    }

    private fun calculateGoalPercent(current: Double, goal: Double): String {
        if (goal <= 0) return "0% Goal"
        val percent = (current / goal * 100).toInt().coerceIn(0, 100)
        val goalInt = goal.toInt()
        return "$percent% to ${goalInt}kg Goal"
    }
}
