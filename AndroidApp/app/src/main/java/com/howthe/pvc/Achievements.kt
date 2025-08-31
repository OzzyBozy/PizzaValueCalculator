package com.howthe.pvc

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.edit

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val target: Int = 1,
    var progress: Int = 0,
    var unlocked: Boolean = false
)

class Achievements private constructor() {

    private val achievementList = mutableListOf<Achievement>()
    private var areAchievementsLoaded = false

    private fun ensureAchievementsLoaded(context: Context) {
        if (!areAchievementsLoaded) {
            val appContext = context.applicationContext
            achievementList.clear()
            achievementList.addAll(
                listOf(
                    Achievement(
                        "enable_achievements",
                        "Achiever!",
                        "Enable achievements from the settings menu"
                    ),
                    Achievement(
                        "infinity",
                        "Endless Appetite",
                        "Witness infinity while comparing pizzas"
                    ),
                    Achievement("zero", "Nothing to See Here", "Hit zero when comparing pizzas"),
                    Achievement("slicer1", "Slicer I: First Slice", "Slice a pizza like a pro"),
                    Achievement(
                        "slicer2",
                        "Slicer II: Beginner",
                        "Slice pizzas 10 times with grace",
                        target = 10
                    ),
                    Achievement(
                        "slicer3",
                        "Slicer III: Novice",
                        "Slice pizzas 50 times without breaking a sweat",
                        target = 50
                    ),
                    Achievement(
                        "slicer4",
                        "Slicer IV: Pizza Ninja",
                        "Slice pizzas 100 times with stealth",
                        target = 100
                    ),
                    Achievement(
                        "multilingual",
                        "Mr. Worldwide",
                        "Make your app speak another language"
                    ),
                    Achievement(
                        "darkmode",
                        "Shade Master",
                        "Toggle between dark and light mode"
                    ),
                    Achievement(
                        "balanced",
                        "Perfectly Balanced",
                        "Discover equal pizza values, as all things should be"
                    ),
                    Achievement(
                        "customer1",
                        "Customer I: Welcome",
                        "Welcome aboard! Opened the app for the first time",
                        target = 1
                    ),
                    Achievement(
                        "customer2",
                        "Customer II: Frequent Visitor",
                        "Back again! You just can’t get enough",
                        target = 5
                    ),
                    Achievement(
                        "customer3",
                        "Customer III: Regular",
                        "Regular! You’re starting to feel at home",
                        target = 10
                    ),
                    Achievement(
                        "customer4",
                        "Customer IV: Veteran",
                        "Veteran! Your dedication is impressive",
                        target = 20
                    ),
                    Achievement(
                        "customer5",
                        "Customer V: Legend",
                        "Legendary Patron! You are basically family now",
                        target = 50
                    )
                )
            )

            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            achievementList.forEach {
                it.unlocked = prefs.getBoolean("${it.id}_unlocked", false)
                it.progress = prefs.getInt("${it.id}_progress", 0)
            }
            areAchievementsLoaded = true
        }
    }

    fun unlock(context: Context, id: String) {
        val appContext = context.applicationContext
        ensureAchievementsLoaded(appContext)
        val ach = achievementList.find { it.id == id } ?: return
        if (!ach.unlocked) {
            ach.unlocked = true
            save(appContext, ach)
            showToast(appContext, ach)
        }
    }

    fun increment(context: Context, id: String, amount: Int = 1) {
        val appContext = context.applicationContext
        ensureAchievementsLoaded(appContext)
        val ach = achievementList.find { it.id == id } ?: return
        if (!ach.unlocked) {
            ach.progress += amount
            if (ach.progress >= ach.target) {
                unlock(appContext, id)
            } else {
                save(appContext, ach)
            }
        }
    }

    fun getAll(context: Context): List<Achievement> {
        ensureAchievementsLoaded(context.applicationContext)
        return achievementList.toList()
    }

    private fun save(context: Context, achievement: Achievement) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean("${achievement.id}_unlocked", achievement.unlocked)
            putInt("${achievement.id}_progress", achievement.progress)
        }
    }

    private fun showToast(context: Context, achievement: Achievement) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.toast, null, false)

        val textView = layout.findViewById<TextView>(R.id.toast_message)
        textView.text = "Achievement Unlocked:\n${achievement.title}"

        val iconView = layout.findViewById<ImageView>(R.id.toast_icon)
        val iconName = "ach_${achievement.id}_pizza"
        var iconResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        if (iconResId == 0) {
            iconResId = context.resources.getIdentifier(
                "ach_default_pizza",
                "drawable",
                context.packageName
            )
        }
        if (iconResId != 0) {
            iconView.setImageResource(iconResId)
        }

        val toast = Toast(context).apply {
            duration = Toast.LENGTH_LONG
            view = layout
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 100)
        }

        toast.show()
    }


    companion object {
        @Volatile
        private var INSTANCE: Achievements? = null
        private const val PREFS_NAME = "ACHIEVEMENT_PREFS"

        fun getInstance(): Achievements {
            return INSTANCE ?: synchronized(this) {
                val instance = Achievements()
                INSTANCE = instance
                instance
            }
        }
    }
}
