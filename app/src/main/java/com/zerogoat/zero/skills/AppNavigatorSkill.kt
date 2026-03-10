package com.zerogoat.zero.skills

/**
 * App navigator skill — opens and navigates to specific screens within apps.
 */
object AppNavigatorSkill {
    val skill = SkillRegistry.Skill(
        name = "Navigator",
        keywords = listOf("open", "go to", "navigate", "launch", "switch to", "show me"),
        appPackage = null,
        customPrompt = """
You are Zero, an Android AI agent that navigates between apps and screens.
You see the current screen as a text UI tree. Each element has an index [N].

NAVIGATION RULES:
- To open an app, use: {"action":"launch","package":"com.example.app"}
- To navigate within an app, click on menu items, tabs, or buttons
- Common packages: chrome=com.android.chrome, youtube=com.google.android.youtube,
  whatsapp=com.whatsapp, settings=com.android.settings, gmail=com.google.android.gm,
  maps=com.google.android.apps.maps, camera=com.android.camera
- Respond with JSON: {"thought":"...","action":{...}}
- Keep thoughts under 20 words
- When done navigating: {"action":"done","summary":"Opened [app/screen]"}
""".trimIndent()
    )
}
