# Implementation Plan: Analytics Dashboard Expansion, Multi-Turn Voice AI Slot-Filling, Clean Transaction Descriptions, Universal High-Precision GPS & Home "Statements" Card Update

## 1. Overview
This plan upgrades five key areas of **Bharat Finance Vault**:
1. **Rich Analytics & Financial Performance Dashboard**: Expand `AnalyticsScreen` with category-wise spending breakdown charts and percentages, monthly income vs. expense trend and net cash flow visualization, daily spending velocity / average ticket size / budget utilization metrics, and account-wise outflow comparisons alongside investment vs. debt health gauges.
2. **Multi-Turn Voice AI Follow-Up Questions (Missing Category, Account, or Amount)**: Enable the Voice AI to detect when any required transaction slot (**Amount**, **Category**, or **Account**) is missing or has multiple options, and ask back interactive spoken + on-screen follow-up questions to fill the missing details before recording the transaction.
3. **Clean Transaction History (Remove Voice Transcript Clutter)**: Remove `"Voice Expense: ... (<spoken transcript>)"` prefixes and raw voice transcripts from transaction descriptions so voice-recorded transactions look clean and natural alongside manually entered expenses.
4. **High-Precision Live GPS Street, Area & Coordinates on Both Voice AND Manual Transactions**: Upgrade `LocationHelper` to fetch live `PRIORITY_HIGH_ACCURACY` GPS street, area, and coordinates, and automatically attach them to **both** voice-recorded transactions and normal manual entry transactions (`RecordTransactionDialog` / `recordTransaction`).
5. **Main Dashboard Home Screen "Statements" Card Refinement**: Update the `"Indian Financial Statements & Ledgers"` card on the Home screen so its title and font cleanly read **"Statements"**, and display the current **Balance** and active report summary (`Balance Sheet / Net Worth & P&L status`).

---

## 2. Main Dashboard Home Screen "Statements" Card (`HomeAndAccountsScreens.kt`)
- **Concise Title & Refined Typography**:
  - Change the card label from `"Indian Financial Statements & Ledgers"` to **"Statements"** with a clean, refined font style (`MaterialTheme.typography.titleSmall`, `FontWeight.SemiBold`).
- **Live Balance & Current Report Status**:
  - Display the current **Balance** (`Net Worth / Total Assets Balance: ₹...`) and current report status (`Balance Sheet: Balanced • Net P&L: ₹...`) directly inside the card and right-side status badge so the user immediately sees the current report balance at a glance.

---

## 3. Expanded Analytics & Financial Performance Dashboard (`ReportsAndAnalyticsScreens.kt`)
- **Daily Spending Velocity, Average Ticket Size & Budget Utilization Strip**:
  - Compute and display **Daily Spending Velocity** (`₹/day` over the active fiscal/month window), **Average Ticket Size** (`Total Expense / Expense Count`), **Transaction Frequency**, and **Overall Budget Utilization %** in a responsive KPI strip.
- **Category-Wise Spending Breakdown Chart & Percentages**:
  - Add a custom Jetpack Compose **Multi-Segment Donut Chart** (`Canvas`) paired with ranked category breakdown rows showing each category's color badge, total ₹ spent, transaction count, and exact percentage share (`%`) of total outflow.
- **Monthly Income vs. Expense Trend & Net Cash Flow Chart**:
  - Build a **Monthly Cash Flow Trend Chart** displaying month-by-month **Income** (Emerald bars), **Expense** (Coral bars), and **Net Cash Flow Surplus/Deficit** indicators across the last 6 active months / fiscal year.
- **Budget Utilization & Day-of-Week Spending Distribution**:
  - Add a **Budget Utilization Breakdown Card** comparing each active budget's limit against actual category spend with color-coded progress bars (`Safe`, `Near Limit`, `Exceeded`).
  - Include a **Day-of-Week Spending Velocity Bar Chart** (`Mon`–`Sun`) highlighting peak spending days.
- **Account-Wise Outflow Comparison & Investment vs. Debt Health Gauges**:
  - Add an **Account-Wise Outflow & Inflow Comparison Card** showing each bank/UPI/cash account's share of total spending, inflow, and net movement.
  - Add **Investment vs. Debt Health Gauges** displaying Asset-to-Debt Coverage Ratio, Liquid-to-Liability Cushion, and Portfolio vs. Loan Principal visual progress bars.

---

## 4. Multi-Turn Voice AI Follow-Up Questions for Missing Slots (`GeminiVaultAssistant.kt` & `FinanceVaultViewModel.kt`)
- **Explicit Slot Detection**:
  - Track whether a spoken transaction command explicitly supplied:
    1. **Amount** (`amountPaise > 0L` — e.g., if the user says *"Record an expense for dinner at Starbucks"* without an amount).
    2. **Category** (`isCategoryExplicit: Boolean` — instead of silently defaulting to `"Household & Groceries"` when no category keyword or historical pattern matches, mark the category as unfulfilled when the user says *"Spent 500 rupees"*).
    3. **Account** (when multiple accounts exist and neither the spoken command nor a learned historical pattern uniquely identifies the account).
- **Sequential Follow-Up Prompting (`DisambiguationStage`)**:
  - Extend `PendingVoiceDisambiguation` to support sequential slot completion:
    - **Missing Amount**: Voice AI asks *"How much was the transaction for?"* and listens for a spoken amount (or lets the user tap a quick amount).
    - **Missing or Ambiguous Category**: Voice AI asks *"Which category should I use for ₹X?"*, displays selectable category chips (`Food & Dining`, `Household & Groceries`, `Transportation & Fuel`, `Utilities & Recharge`, `Shopping & Lifestyle`, `Healthcare & Medical`, `Entertainment & Media`, etc.), and automatically opens the microphone so the user can speak the category name or option number.
    - **Missing or Ambiguous Account**: Voice AI asks *"Which account should I use for this ₹X <Category> transaction?"*, displays the matching accounts, and listens for the user's spoken reply or tap.
  - Once all required fields (**Amount**, **Category**, and **Account**) are fulfilled, the transaction is immediately saved and confirmed via TTS.

---

## 5. Clean Voice Transaction Descriptions (`GeminiVaultAssistant.kt` & `FinanceVaultViewModel.kt`)
- **Eliminate Raw Transcript Clutter**:
  - Update `buildCleanNarration` and AI JSON response normalization in `GeminiVaultAssistant.kt` so that `description` never includes `"Voice Expense:"`, `"Voice Income:"`, `"Voice Transfer:"`, or the parenthesized `(rawText)` voice transcript.
  - Format descriptions cleanly just like normal transactions—using the extracted item/merchant/counterparty name (e.g., `"Coffee • Starbucks"`, `"Groceries"`, or the clean category title `"Food & Dining"` when no extra note was spoken).

---

## 6. High-Precision Live GPS Location for Both Voice AND Manual Transactions (`LocationAndAuthHelper.kt`, `VaultCommonComponents.kt`, `MainActivity.kt` & `FinanceVaultViewModel.kt`)
- **High-Accuracy GPS & Street-Level Reverse Geocoding (`LocationAndAuthHelper.kt`)**:
  - Upgrade `LocationHelper.fetchCurrentLocation` to use `CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).setMaxUpdateAgeMillis(10_000L).setDurationMillis(3_500L).build()` with a fallback to the freshest high-accuracy fix across `GPS_PROVIDER` and `NETWORK_PROVIDER`.
  - Remove the hardcoded `"Connaught Place, New Delhi"` fallback coordinate so only genuine device location fixes are used.
  - Format `placeName` in `LocationHelper` to include the street/thoroughfare (`thoroughfare` / `subThoroughfare`), landmark/premise (`featureName`), area/neighborhood (`subLocality`), city (`locality`), and formatted GPS coordinates (`lat, lng`).
- **Normal Manual Entry Transactions (`VaultCommonComponents.kt` & `FinanceVaultViewModel.kt`)**:
  - In `RecordTransactionDialog`, automatically fetch the live high-accuracy GPS street, area, and coordinates when opening the dialog (or when pressing the GPS icon or submitting).
  - If the user enters a custom merchant/place name manually in the Location box (e.g., `"Reliance Fresh"`), combine it with the live GPS street, area, and coordinates (`"Reliance Fresh • <Street, Area> (<lat>, <lng>)"`) on submit; if left blank, automatically populate the full live GPS street, area, and coordinates.
  - In `FinanceVaultViewModel.recordTransaction`, if a manual transaction is submitted while GPS is still resolving, fetch and attach the live GPS street/area and coordinates before writing to Room.
- **Voice Transactions (`MainActivity.kt` & `FinanceVaultViewModel.kt`)**:
  - Request `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` alongside `RECORD_AUDIO` when starting voice commands if not yet granted, and always attach live GPS street, area, and coordinates to every voice-recorded transaction.
