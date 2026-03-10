package com.zerogoat.zero.skills

object GenericSkill {
    val skill = SkillRegistry.Skill(
        name = "Generic",
        keywords = emptyList(),
        appPackage = null,
        customPrompt = null // Uses the default PromptBuilder.SYSTEM_PROMPT
    )
}
