# Voice Microphone 4-Second Grace Timer, Instant Mute/Cancel & Full Voice Deletion Fix

This plan upgrades the on-demand Voice Command controller so the microphone gives you a full **3 to 4 second (4,000 ms) grace period** both before you begin speaking and after you pause speaking before auto-closing, guarantees that tapping the microphone/mute button immediately stops listening, cancels processing, and mutes any spoken reply, and fixes voice delete commands across all items (transactions, accounts, subscriptions, budgets, goals, and loans).

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during clarification and govern this update:

- **Confirmed Decision 1 — 4-Second Pre-Speech & Post-Speech Grace Timer with Auto-Close**: When you tap the microphone button, the microphone waits a full **4 seconds** for you to start speaking and waits **4 seconds** after you pause speaking before finalizing your command and auto-closing. If Android's speech recognizer closes early during a brief pause before 4 seconds have elapsed, the controller seamlessly holds the 4-second window open (accumulating any partial transcript) and auto-closes cleanly once the 4-second silence window expires.
- **Confirmed Decision 2 — Smart Voice Deletion with Multi-Option Disambiguation**: Saying *"Delete transaction"*, *"Delete my coffee expense"*, *"Delete subscription"*, *"Delete budget"*, *"Delete savings goal"*, or *"Delete account"* will immediately delete/reverse the item if only one matches (or the latest one if unspecified), or ask you aloud and on screen which item to delete when multiple items match—with full *"Undo"* support.
- **Confirmed Decision 3 — Instant All-State Mute & Cancel Button**: Tapping the microphone/mute button while the app is **listening**, **processing ("Recording...")**, or **speaking a reply** immediately stops the microphone, cancels any in-flight command processing, clears any pending follow-up prompt, and mutes `TextToSpeech` output.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **4-Second Pre- & Post-Speech Delay & Auto-Close**: Gives users natural time to think before speaking and to pause mid-thought (e.g., recalling an amount or merchant name) without the microphone cutting off prematurely, then automatically closes after 4 seconds of silence.
  2. **Unbreakable Microphone & Mute Button**: Fixes the state deadlock where `isProcessingCommand` ("Recording...") blocked the mute/stop button and prevented subsequent voice sessions if a command took too long or failed.
  3. **Full Voice Deletion Engine**: Adds instant local & AI parsing for deleting/removing any transaction (by keyword, category, amount, or latest), account, subscription, budget, or savings goal—with multi-option voice disambiguation when multiple records match.
- **Target Audience / Persona**: Users logging or managing finances hands-free who pause for a couple of seconds while recalling transaction details or who want to delete an entry or mute the assistant at any moment.
- **Key Value**: Eliminates premature microphone cutoffs, fixes the "Recording..." freeze on delete commands, and ensures the mute button responds instantaneously in every state.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Delayed Speech with 4-Second Auto-Close**:
     - User taps the right-side floating Microphone button.
     - The floating status pill shows **"Listening (4s grace)... Speak now"**.
     - If the user waits 2.5 seconds before speaking *"Delete my last grocery transaction"*, the microphone stays open, captures the full sentence, waits up to 4 seconds after the user finishes speaking in case they add more words, and then auto-closes and executes the command.
     - If the user says nothing for 4 seconds, the microphone auto-closes cleanly to save battery.
  2. **Deleting Any Item by Voice**:
     - User says *"Delete transaction"*, *"Delete 450 transaction"*, *"Delete food expense"*, *"Delete Netflix"*, *"Delete grocery budget"*, or *"Delete account"*.
     - If 1 item matches (or the user asked to delete the latest transaction), it is reversed/deleted immediately and the app speaks confirmation (*"Deleted ₹450 Food & Dining transaction. Say Undo to restore it."*).
     - If multiple items match (e.g., 3 Food & Dining transactions or 2 budgets), the app displays the **Voice Disambiguation Card** and asks aloud which one to delete.
  3. **Instant Mute / Stop Anytime**:
     - Anytime the floating button is active (Listening, Processing, or Speaking a reply), the button shows the **Coral `MicOff` (Mute/Stop)** state. Tapping it immediately cancels the active operation, silences speech, and resets the microphone to idle.
- **Visual Identity & Theme**:
  - Preserves the **Obsidian & Emerald** theme (`VaultObsidianBg`, `VaultEmerald`, `VaultCoral`, `VaultGold`, `VaultCyan`) and the right-side circular floating microphone button above the bottom navigation bar.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Two-Layer 4,000 ms Silence & Auto-Close Controller**
  - *Chosen Approach*: Configure `RecognizerIntent` with 4,000 ms silence parameters (`EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS = 4000L`, `EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS = 4000L`, `EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS = 4000L`) **plus** an explicit `Handler`-backed 4,000 ms silence watchdog in `VoiceTransactionController`.
  - *Why*: Many Android OEM speech engines ignore `RecognizerIntent` extras and fire `ERROR_SPEECH_TIMEOUT` or `onResults` after only 1.2 seconds of silence. By combining a 4-second pre-speech window (automatically continuing listening if the OS times out before 4 seconds) and a 4-second post-speech buffer (committing the accumulated transcript after 4 seconds of silence following speech), the microphone reliably waits 3–4 seconds on all Android devices and then auto-closes.
- **Decision 2: Cancellable Coroutine Job & `try / finally` State Reset for Voice Execution**
  - *Chosen Approach*: Track `activeVoiceCommandJob: Job?` in `FinanceVaultViewModel` and wrap `executeVoiceOrGeminiCommand` in a `try / finally` block that always invokes `onFinished` unless explicitly cancelled by the user's mute button.
  - *Why*: Guarantees `isProcessingCommand` can never get stuck on `"Recording..."` and allows tapping the mute button to cancel a running command immediately.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌───────────────────────────────────────────────────────────────────────────┐
│                Right-Side Floating Mic / Mute Button (FAB)                │
│  • Idle -> Tap starts 4s Listening Session                                │
│  • Listening / Processing / Speaking -> Tap Mutes & Cancels Immediately   │
└───────────────────────────────┬───────────────────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      VoiceTransactionController                           │
│  • Pre-Speech Timer (4,000 ms): Keeps mic alive up to 4s before speech    │
│  • Post-Speech Timer (4,000 ms): Buffers segments & auto-closes after 4s  │
│  • stopAndMuteAll(): Cancels recognizer, TTS, timers & active VM Job      │
└───────────────────────────────┬───────────────────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────────┐
│             GeminiVaultAssistant + FinanceVaultViewModel                  │
│  • Instant Local Delete Parser: DELETE_TRANSACTION, DELETE_SUBSCRIPTION,  │
│    DELETE_BUDGET, DELETE_GOAL, ARCHIVE_ACCOUNT, REVERSE_LAST_TRANSACTION  │
│  • Multi-Option Disambiguation when >1 item matches + Full Voice Undo     │
└───────────────────────────────────────────────────────────────────────────┘
```

- **Interactive Component & State Mapping**:
  - `VoiceTransactionController`:
    - Tracks `accumulatedTranscript` and `lastSpeechActivityTimeMs`.
    - Enforces a 4,000 ms pre-speech timeout and a 4,000 ms post-speech silence timeout before finalizing and auto-closing the microphone.
    - Exposes `stopAndMuteAll()` so tapping the button during `isActivelyListening`, `isProcessingCommand`, or `isSpeakingReply` immediately stops the mic, cancels the ViewModel job, and stops `TextToSpeech`.
  - `GeminiVaultAssistant` & `FinanceVaultViewModel`:
    - Adds `DELETE_TRANSACTION`, `DELETE_BUDGET`, and `DELETE_GOAL` alongside `DELETE_SUBSCRIPTION`, `ARCHIVE_ACCOUNT`, and `REVERSE_LAST_TRANSACTION`.
    - Matches transactions by keyword, category, merchant, or amount; if multiple transactions match, presents up to 5 matching transactions in the `VoiceDisambiguationOverlayCard` and speaks the options aloud; if only one matches (or generic *"delete transaction"*), reverses it immediately and stores an `UndoableVoiceAction`.
