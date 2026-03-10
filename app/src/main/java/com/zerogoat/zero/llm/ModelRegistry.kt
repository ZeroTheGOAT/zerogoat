package com.zerogoat.zero.llm

/**
 * Registry of 100+ LLM models across all major providers.
 * Accessible via OpenRouter (single API key for all) or direct provider APIs.
 */
object ModelRegistry {

    data class ModelInfo(
        val id: String,            // OpenRouter model ID (e.g., "google/gemini-2.0-flash-exp")
        val name: String,          // Display name
        val provider: String,      // Provider name
        val category: Category,    // Quick-pick category
        val contextWindow: Int,    // Max tokens
        val supportsVision: Boolean = false,
        val supportsStreaming: Boolean = true,
        val inputCostPer1M: Double,    // $ per 1M input tokens
        val outputCostPer1M: Double,   // $ per 1M output tokens
        val isFree: Boolean = false
    )

    enum class Category { FREE, FAST, SMART, REASONING, VISION, CODING, LOCAL }

    /** All available models — grouped by provider */
    val allModels: List<ModelInfo> = listOf(
        // ===== FREE MODELS =====
        ModelInfo("google/gemma-2-9b-it:free", "Gemma 2 9B", "Google", Category.FREE, 8192, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),
        ModelInfo("meta-llama/llama-3.1-8b-instruct:free", "Llama 3.1 8B", "Meta", Category.FREE, 131072, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),
        ModelInfo("mistralai/mistral-7b-instruct:free", "Mistral 7B", "Mistral", Category.FREE, 32768, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),
        ModelInfo("qwen/qwen-2.5-7b-instruct:free", "Qwen 2.5 7B", "Qwen", Category.FREE, 32768, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),
        ModelInfo("huggingfaceh4/zephyr-7b-beta:free", "Zephyr 7B", "HuggingFace", Category.FREE, 4096, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),
        ModelInfo("nousresearch/hermes-3-llama-3.1-405b:free", "Hermes 3 405B", "Nous", Category.FREE, 131072, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),

        // ===== GOOGLE =====
        ModelInfo("google/gemini-2.0-flash-exp", "Gemini 2.0 Flash", "Google", Category.FAST, 1048576, supportsVision = true, inputCostPer1M = 0.075, outputCostPer1M = 0.30),
        ModelInfo("google/gemini-2.0-flash-thinking-exp", "Gemini 2.0 Flash Thinking", "Google", Category.REASONING, 1048576, inputCostPer1M = 0.075, outputCostPer1M = 0.30),
        ModelInfo("google/gemini-pro-1.5", "Gemini 1.5 Pro", "Google", Category.SMART, 2097152, supportsVision = true, inputCostPer1M = 1.25, outputCostPer1M = 5.0),
        ModelInfo("google/gemini-flash-1.5", "Gemini 1.5 Flash", "Google", Category.FAST, 1048576, supportsVision = true, inputCostPer1M = 0.075, outputCostPer1M = 0.30),
        ModelInfo("google/gemma-2-27b-it", "Gemma 2 27B", "Google", Category.FAST, 8192, inputCostPer1M = 0.27, outputCostPer1M = 0.27),

        // ===== OPENAI =====
        ModelInfo("openai/gpt-4o", "GPT-4o", "OpenAI", Category.SMART, 128000, supportsVision = true, inputCostPer1M = 2.50, outputCostPer1M = 10.0),
        ModelInfo("openai/gpt-4o-mini", "GPT-4o Mini", "OpenAI", Category.FAST, 128000, supportsVision = true, inputCostPer1M = 0.15, outputCostPer1M = 0.60),
        ModelInfo("openai/o1", "o1", "OpenAI", Category.REASONING, 200000, inputCostPer1M = 15.0, outputCostPer1M = 60.0),
        ModelInfo("openai/o1-mini", "o1 Mini", "OpenAI", Category.REASONING, 128000, inputCostPer1M = 3.0, outputCostPer1M = 12.0),
        ModelInfo("openai/o3-mini", "o3 Mini", "OpenAI", Category.REASONING, 200000, inputCostPer1M = 1.10, outputCostPer1M = 4.40),
        ModelInfo("openai/gpt-4-turbo", "GPT-4 Turbo", "OpenAI", Category.SMART, 128000, supportsVision = true, inputCostPer1M = 10.0, outputCostPer1M = 30.0),
        ModelInfo("openai/chatgpt-4o-latest", "ChatGPT-4o Latest", "OpenAI", Category.SMART, 128000, supportsVision = true, inputCostPer1M = 5.0, outputCostPer1M = 15.0),

        // ===== ANTHROPIC =====
        ModelInfo("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", "Anthropic", Category.SMART, 200000, supportsVision = true, inputCostPer1M = 3.0, outputCostPer1M = 15.0),
        ModelInfo("anthropic/claude-3.5-haiku", "Claude 3.5 Haiku", "Anthropic", Category.FAST, 200000, supportsVision = true, inputCostPer1M = 0.80, outputCostPer1M = 4.0),
        ModelInfo("anthropic/claude-3-opus", "Claude 3 Opus", "Anthropic", Category.SMART, 200000, supportsVision = true, inputCostPer1M = 15.0, outputCostPer1M = 75.0),
        ModelInfo("anthropic/claude-3-haiku", "Claude 3 Haiku", "Anthropic", Category.FAST, 200000, supportsVision = true, inputCostPer1M = 0.25, outputCostPer1M = 1.25),

        // ===== META (LLAMA) =====
        ModelInfo("meta-llama/llama-3.1-405b-instruct", "Llama 3.1 405B", "Meta", Category.SMART, 131072, inputCostPer1M = 2.70, outputCostPer1M = 2.70),
        ModelInfo("meta-llama/llama-3.1-70b-instruct", "Llama 3.1 70B", "Meta", Category.SMART, 131072, inputCostPer1M = 0.52, outputCostPer1M = 0.75),
        ModelInfo("meta-llama/llama-3.1-8b-instruct", "Llama 3.1 8B", "Meta", Category.FAST, 131072, inputCostPer1M = 0.055, outputCostPer1M = 0.055),
        ModelInfo("meta-llama/llama-3.2-90b-vision-instruct", "Llama 3.2 90B Vision", "Meta", Category.VISION, 131072, supportsVision = true, inputCostPer1M = 0.90, outputCostPer1M = 0.90),
        ModelInfo("meta-llama/llama-3.2-11b-vision-instruct", "Llama 3.2 11B Vision", "Meta", Category.VISION, 131072, supportsVision = true, inputCostPer1M = 0.055, outputCostPer1M = 0.055),
        ModelInfo("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B", "Meta", Category.SMART, 131072, inputCostPer1M = 0.12, outputCostPer1M = 0.30),

        // ===== MISTRAL =====
        ModelInfo("mistralai/mistral-large", "Mistral Large", "Mistral", Category.SMART, 128000, inputCostPer1M = 2.0, outputCostPer1M = 6.0),
        ModelInfo("mistralai/mistral-medium", "Mistral Medium", "Mistral", Category.SMART, 32768, inputCostPer1M = 2.75, outputCostPer1M = 8.10),
        ModelInfo("mistralai/mistral-small", "Mistral Small", "Mistral", Category.FAST, 32768, inputCostPer1M = 0.20, outputCostPer1M = 0.60),
        ModelInfo("mistralai/mistral-tiny", "Mistral Tiny", "Mistral", Category.FAST, 32768, inputCostPer1M = 0.25, outputCostPer1M = 0.25),
        ModelInfo("mistralai/mixtral-8x7b-instruct", "Mixtral 8x7B", "Mistral", Category.FAST, 32768, inputCostPer1M = 0.24, outputCostPer1M = 0.24),
        ModelInfo("mistralai/mixtral-8x22b-instruct", "Mixtral 8x22B", "Mistral", Category.SMART, 65536, inputCostPer1M = 0.65, outputCostPer1M = 0.65),
        ModelInfo("mistralai/codestral-latest", "Codestral", "Mistral", Category.CODING, 32768, inputCostPer1M = 0.30, outputCostPer1M = 0.90),

        // ===== DEEPSEEK =====
        ModelInfo("deepseek/deepseek-chat", "DeepSeek V3", "DeepSeek", Category.SMART, 131072, inputCostPer1M = 0.14, outputCostPer1M = 0.28),
        ModelInfo("deepseek/deepseek-r1", "DeepSeek R1", "DeepSeek", Category.REASONING, 131072, inputCostPer1M = 0.55, outputCostPer1M = 2.19),
        ModelInfo("deepseek/deepseek-r1-distill-llama-70b", "DeepSeek R1 Llama 70B", "DeepSeek", Category.REASONING, 131072, inputCostPer1M = 0.36, outputCostPer1M = 0.90),

        // ===== COHERE =====
        ModelInfo("cohere/command-r-plus", "Command R+", "Cohere", Category.SMART, 128000, inputCostPer1M = 2.50, outputCostPer1M = 10.0),
        ModelInfo("cohere/command-r", "Command R", "Cohere", Category.FAST, 128000, inputCostPer1M = 0.15, outputCostPer1M = 0.60),

        // ===== PERPLEXITY =====
        ModelInfo("perplexity/sonar-pro", "Sonar Pro", "Perplexity", Category.SMART, 200000, inputCostPer1M = 3.0, outputCostPer1M = 15.0),
        ModelInfo("perplexity/sonar", "Sonar", "Perplexity", Category.FAST, 127072, inputCostPer1M = 1.0, outputCostPer1M = 1.0),
        ModelInfo("perplexity/sonar-reasoning", "Sonar Reasoning", "Perplexity", Category.REASONING, 127072, inputCostPer1M = 1.0, outputCostPer1M = 5.0),

        // ===== QWEN =====
        ModelInfo("qwen/qwen-2.5-72b-instruct", "Qwen 2.5 72B", "Qwen", Category.SMART, 131072, inputCostPer1M = 0.36, outputCostPer1M = 0.36),
        ModelInfo("qwen/qwen-2.5-coder-32b-instruct", "Qwen 2.5 Coder 32B", "Qwen", Category.CODING, 32768, inputCostPer1M = 0.18, outputCostPer1M = 0.18),
        ModelInfo("qwen/qvq-72b-preview", "QVQ 72B Vision", "Qwen", Category.VISION, 131072, supportsVision = true, inputCostPer1M = 0.36, outputCostPer1M = 0.36),

        // ===== NOUS RESEARCH =====
        ModelInfo("nousresearch/hermes-3-llama-3.1-405b", "Hermes 3 405B", "Nous", Category.SMART, 131072, inputCostPer1M = 2.70, outputCostPer1M = 2.70),

        // ===== MICROSOFT =====
        ModelInfo("microsoft/phi-4", "Phi-4", "Microsoft", Category.FAST, 16384, inputCostPer1M = 0.07, outputCostPer1M = 0.14),
        ModelInfo("microsoft/wizardlm-2-8x22b", "WizardLM 2 8x22B", "Microsoft", Category.SMART, 65536, inputCostPer1M = 0.50, outputCostPer1M = 0.50),

        // ===== X.AI =====
        ModelInfo("x-ai/grok-2", "Grok 2", "xAI", Category.SMART, 131072, inputCostPer1M = 2.0, outputCostPer1M = 10.0),
        ModelInfo("x-ai/grok-2-vision", "Grok 2 Vision", "xAI", Category.VISION, 32768, supportsVision = true, inputCostPer1M = 2.0, outputCostPer1M = 10.0),
        ModelInfo("x-ai/grok-beta", "Grok Beta", "xAI", Category.SMART, 131072, inputCostPer1M = 5.0, outputCostPer1M = 15.0),

        // ===== NVIDIA =====
        ModelInfo("nvidia/llama-3.1-nemotron-70b-instruct", "Nemotron 70B", "NVIDIA", Category.SMART, 131072, inputCostPer1M = 0.12, outputCostPer1M = 0.30),

        // ===== AMAZON =====
        ModelInfo("amazon/nova-pro-v1", "Nova Pro", "Amazon", Category.SMART, 300000, supportsVision = true, inputCostPer1M = 0.80, outputCostPer1M = 3.20),
        ModelInfo("amazon/nova-lite-v1", "Nova Lite", "Amazon", Category.FAST, 300000, supportsVision = true, inputCostPer1M = 0.06, outputCostPer1M = 0.24),
        ModelInfo("amazon/nova-micro-v1", "Nova Micro", "Amazon", Category.FAST, 128000, inputCostPer1M = 0.035, outputCostPer1M = 0.14),

        // ===== AI21 =====
        ModelInfo("ai21/jamba-1.5-large", "Jamba 1.5 Large", "AI21", Category.SMART, 256000, inputCostPer1M = 2.0, outputCostPer1M = 8.0),
        ModelInfo("ai21/jamba-1.5-mini", "Jamba 1.5 Mini", "AI21", Category.FAST, 256000, inputCostPer1M = 0.20, outputCostPer1M = 0.40),

        // ===== INFLECTION =====
        ModelInfo("inflection/inflection-3-pi", "Pi 3", "Inflection", Category.SMART, 8000, inputCostPer1M = 0.60, outputCostPer1M = 2.40),
        ModelInfo("inflection/inflection-3-productivity", "Inflection 3 Productivity", "Inflection", Category.SMART, 8000, inputCostPer1M = 0.60, outputCostPer1M = 2.40),

        // ===== EVA =====
        ModelInfo("eva-unit-01/eva-qwen-2.5-72b", "EVA Qwen 72B", "EVA", Category.SMART, 131072, inputCostPer1M = 0.40, outputCostPer1M = 0.60),

        // ===== CODING MODELS =====
        ModelInfo("deepseek/deepseek-coder", "DeepSeek Coder", "DeepSeek", Category.CODING, 131072, inputCostPer1M = 0.14, outputCostPer1M = 0.28),
        ModelInfo("01-ai/yi-coder-9b-chat", "Yi Coder 9B", "01.AI", Category.CODING, 128000, inputCostPer1M = 0.10, outputCostPer1M = 0.10),

        // ===== VISION MODELS =====
        ModelInfo("openai/gpt-4-vision-preview", "GPT-4 Vision", "OpenAI", Category.VISION, 128000, supportsVision = true, inputCostPer1M = 10.0, outputCostPer1M = 30.0),

        // ===== LOCAL / OLLAMA =====
        ModelInfo("ollama/llama3.1:8b", "Llama 3.1 8B (Local)", "Ollama", Category.LOCAL, 131072, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true, supportsStreaming = true),
        ModelInfo("ollama/gemma2:9b", "Gemma 2 9B (Local)", "Ollama", Category.LOCAL, 8192, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),
        ModelInfo("ollama/phi3:mini", "Phi-3 Mini (Local)", "Ollama", Category.LOCAL, 4096, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),
        ModelInfo("ollama/mistral:7b", "Mistral 7B (Local)", "Ollama", Category.LOCAL, 32768, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),
        ModelInfo("ollama/qwen2.5:7b", "Qwen 2.5 7B (Local)", "Ollama", Category.LOCAL, 32768, inputCostPer1M = 0.0, outputCostPer1M = 0.0, isFree = true),

        // ===== GROQ (Fast Inference) =====
        ModelInfo("groq/llama-3.3-70b-versatile", "Llama 3.3 70B (Groq)", "Groq", Category.FAST, 131072, inputCostPer1M = 0.59, outputCostPer1M = 0.79),
        ModelInfo("groq/llama-3.1-8b-instant", "Llama 3.1 8B (Groq)", "Groq", Category.FAST, 131072, inputCostPer1M = 0.05, outputCostPer1M = 0.08),
        ModelInfo("groq/mixtral-8x7b-32768", "Mixtral 8x7B (Groq)", "Groq", Category.FAST, 32768, inputCostPer1M = 0.24, outputCostPer1M = 0.24),
        ModelInfo("groq/gemma2-9b-it", "Gemma 2 9B (Groq)", "Groq", Category.FAST, 8192, inputCostPer1M = 0.20, outputCostPer1M = 0.20),
    )

    // ===== Quick-pick helpers =====

    fun getByCategory(category: Category): List<ModelInfo> =
        allModels.filter { it.category == category }

    fun getFreeModels(): List<ModelInfo> = allModels.filter { it.isFree }
    fun getVisionModels(): List<ModelInfo> = allModels.filter { it.supportsVision }
    fun getReasoningModels(): List<ModelInfo> = getByCategory(Category.REASONING)
    fun getCodingModels(): List<ModelInfo> = getByCategory(Category.CODING)
    fun getLocalModels(): List<ModelInfo> = getByCategory(Category.LOCAL)

    fun getByProvider(provider: String): List<ModelInfo> =
        allModels.filter { it.provider.equals(provider, ignoreCase = true) }

    fun getProviders(): List<String> = allModels.map { it.provider }.distinct().sorted()

    fun findById(id: String): ModelInfo? = allModels.find { it.id == id }

    /** Recommended model for Zero agent (cheapest + fast + good) */
    val defaultModel = allModels.first { it.id == "google/gemini-2.0-flash-exp" }

    /** Quick picks for the model selector */
    val quickPicks = listOf(
        "🆓 Free" to getFreeModels().take(5),
        "⚡ Fast" to getByCategory(Category.FAST).sortedBy { it.inputCostPer1M }.take(5),
        "🧠 Smart" to getByCategory(Category.SMART).sortedBy { it.inputCostPer1M }.take(5),
        "💭 Reasoning" to getReasoningModels(),
        "👁️ Vision" to getVisionModels().take(5),
        "💻 Coding" to getCodingModels(),
        "🏠 Local" to getLocalModels()
    )
}
