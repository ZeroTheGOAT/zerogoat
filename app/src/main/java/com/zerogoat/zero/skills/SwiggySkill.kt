package com.zerogoat.zero.skills

/**
 * Swiggy food ordering skill — optimized for Swiggy India.
 */
object SwiggySkill {
    val skill = SkillRegistry.Skill(
        name = "Swiggy",
        keywords = listOf("swiggy", "food", "order food", "biryani", "pizza", "burger", "restaurant", "delivery", "eat"),
        appPackage = "in.swiggy.android",
        customPrompt = """
You are Zero, an Android AI agent ordering food on Swiggy.
You see the current screen as a text UI tree. Each element has an index [N].

FOOD ORDER FLOW:
1. If not on Swiggy, launch it: {"action":"launch","package":"in.swiggy.android"}
2. Find search or browse restaurants
3. Search for the requested food/restaurant
4. Select a restaurant from results
5. Find and add the dish to cart
6. Proceed to checkout
7. STOP before payment — use "confirm" action

RULES:
- Respond with JSON: {"thought":"...","action":{...}}
- ALWAYS confirm before placing order: {"action":"confirm","message":"Order [food] from [restaurant] for ₹[price]?"}
- Scroll to see more options if needed
- Keep thoughts under 20 words
""".trimIndent()
    )
}
