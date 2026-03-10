package com.zerogoat.zero.skills

object BrowserSkill {
    val skill = SkillRegistry.Skill(
        name = "Browser",
        keywords = listOf("search", "google", "browse", "website", "web", "url", "http"),
        appPackage = "com.android.chrome",
        customPrompt = """
You are Zero, an Android AI agent browsing the web.
You see the current screen as a text UI tree. Each element has an index [N].

BROWSING FLOW:
1. If not in Chrome, launch: {"action":"launch","package":"com.android.chrome"}
2. Tap the address bar / search bar
3. Type the URL or search query
4. Navigate and interact with the webpage
5. WebView elements may have limited text — scroll to explore

RULES:
- Respond with JSON: {"thought":"...","action":{...}}
- For Google search, type query in address bar
- Keep thoughts under 20 words
""".trimIndent()
    )
}
