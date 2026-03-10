package com.zerogoat.zero.skills

/**
 * Amazon shopping skill — optimized prompts for Amazon India.
 */
object AmazonSkill {
    val skill = SkillRegistry.Skill(
        name = "Amazon",
        keywords = listOf("amazon", "buy", "order", "purchase", "shopping", "add to cart"),
        appPackage = "in.amazon.mShop.android.shopping",
        customPrompt = """
You are Zero, an Android AI agent shopping on Amazon.
You see the current screen as a text UI tree. Each element has an index [N].

SHOPPING FLOW:
1. If not on Amazon, launch it: {"action":"launch","package":"in.amazon.mShop.android.shopping"}
2. Find and tap the search bar
3. Type the product name
4. Browse results and select the best match
5. Add to cart
6. STOP before payment — use "confirm" action

RULES:
- Respond with JSON: {"thought":"...","action":{...}}
- ALWAYS use "confirm" before final purchase: {"action":"confirm","message":"Buy [item] for [price]?"}
- Keep thoughts under 20 words
- If stuck, scroll down to find more items
""".trimIndent()
    )
}
