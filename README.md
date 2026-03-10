incomplete (gave up after this chigga claude burnt ~200$ credit for this shltty work)
# ZeroGoat 🐐

**Zero — Your JARVIS for Android.**

An open-source AI agent that controls your phone through natural language. Like OpenClaw, but on Android — no root required.

## Features

- 🧠 **AI Agent** — Understands your commands and executes them autonomously
- 📱 **OS-Level Control** — Clicks, types, scrolls, swipes — no root needed
- 🎤 **Voice Control** — "Hey Zero" wake word + continuous listening
- 📡 **WhatsApp & Telegram** — Control Zero from messaging apps
- 💰 **Token-Optimized** — UI tree text instead of screenshots (85% cheaper)
- 🔐 **BYOK** — Bring your own API keys (Gemini, OpenAI, Anthropic)
- 🛒 **Built-in Skills** — Amazon, Swiggy, Settings, Browser, App Navigation

## Quick Start

1. Clone and open in Android Studio
2. Build & install on your Android device (API 28+)
3. Open ZeroGoat → Enter your API key
4. Enable Accessibility Service when prompted
5. Say "Hey Zero, open Chrome" 🚀

## Architecture

```
User Command → SkillRegistry → LLM (Gemini/OpenAI/Claude)
                                      ↓
                              AgentLoop (plan → act → verify)
                                      ↓
                        AccessibilityService → Any App
```

## Token Optimization

| Method | Tokens | Savings |
|---|---|---|
| Screenshot → Vision AI | ~1500 | Baseline |
| UI Tree → Text LLM | ~200 | **85%** |
| Local commands | 0 | **100%** |

## Providers

| Provider | Model | Cost/1M tokens |
|---|---|---|
| Gemini (default) | Flash 2.0 | $0.075 input / $0.30 output |
| OpenAI | GPT-4o-mini | $0.15 / $0.60 |
| Anthropic | Claude 3.5 Haiku | $0.25 / $1.25 |

## License

MIT — Use it, fork it, build on it. 🐐
