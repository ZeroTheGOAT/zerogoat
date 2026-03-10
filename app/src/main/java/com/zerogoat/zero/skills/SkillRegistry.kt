package com.zerogoat.zero.skills

import com.zerogoat.zero.agent.ActionType

/**
 * Registry that matches user commands to specialized skills.
 * Skills optimize prompts and flows for specific apps.
 * Falls back to GenericSkill for unknown tasks.
 */
class SkillRegistry {

    data class Skill(
        val name: String,
        val keywords: List<String>,
        val appPackage: String?,
        val customPrompt: String?
    )

    private val skills = listOf(
        AmazonSkill.skill,
        SwiggySkill.skill,
        AppNavigatorSkill.skill,
        SettingsSkill.skill,
        BrowserSkill.skill
    )

    /** Find the best matching skill for a user command */
    fun matchSkill(command: String): Skill? {
        val lower = command.lowercase()
        return skills.maxByOrNull { skill ->
            skill.keywords.count { keyword -> lower.contains(keyword) }
        }?.takeIf { skill ->
            skill.keywords.any { keyword -> command.lowercase().contains(keyword) }
        }
    }

    /**
     * Try to execute simple commands locally without an API call.
     * Saves 100% tokens for trivial commands.
     */
    fun tryLocalExecution(command: String): ActionType? {
        val lower = command.lowercase().trim()

        // Direct app launches
        val appMap = mapOf(
            "open chrome" to "com.android.chrome",
            "open browser" to "com.android.chrome",
            "open youtube" to "com.google.android.youtube",
            "open whatsapp" to "com.whatsapp",
            "open telegram" to "org.telegram.messenger",
            "open instagram" to "com.instagram.android",
            "open twitter" to "com.twitter.android",
            "open x" to "com.twitter.android",
            "open gmail" to "com.google.android.gm",
            "open maps" to "com.google.android.apps.maps",
            "open google maps" to "com.google.android.apps.maps",
            "open camera" to "com.android.camera",
            "open photos" to "com.google.android.apps.photos",
            "open settings" to "com.android.settings",
            "open play store" to "com.android.vending",
            "open spotify" to "com.spotify.music",
            "open amazon" to "in.amazon.mShop.android.shopping",
            "open swiggy" to "in.swiggy.android",
            "open zomato" to "com.application.zomato",
            "open phone" to "com.android.dialer",
            "open messages" to "com.google.android.apps.messaging",
            "open calculator" to "com.google.android.calculator",
            "open clock" to "com.google.android.deskclock",
            "open calendar" to "com.google.android.calendar",
            "open files" to "com.google.android.documentsui",
            "open netflix" to "com.netflix.mediaclient",
            "open facebook" to "com.facebook.katana",
            "open snapchat" to "com.snapchat.android",
        )

        for ((phrase, pkg) in appMap) {
            if (lower.startsWith(phrase) || lower == phrase) {
                return ActionType.LaunchApp(pkg)
            }
        }

        // Simple navigation commands
        return when {
            lower == "go back" || lower == "back" -> ActionType.Back
            lower == "go home" || lower == "home" -> ActionType.Home
            lower == "recent apps" || lower == "recents" -> ActionType.Recents
            lower == "show notifications" || lower == "notifications" -> ActionType.Notifications
            else -> null
        }
    }
}
