# Finance Vault — Search History, Hold-to-Drag Batch Selection & Header Simplification

This update adds a 5-term recent search history to the top search bar, introduces a 1.5-second long-press and swipe-down batch selection mode for transactions (supporting bulk deletion via double-entry reversal and bulk recategorization), and removes all descriptive subtitles beneath window titles across the app.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and UX decisions were confirmed during interactive clarification and govern this implementation:

- **1.5-Second Hold & Swipe-Down Batch Selection**: Holding a transaction card for 1,500ms activates batch selection mode and selects that transaction; continuing to swipe/drag downward (or upward) across the list selects subsequent transactions in one continuous gesture. Users can also tap additional rows while batch mode is active.
- **Delete via Double-Entry Reversal**: Deleting selected transactions (single or bulk) marks them as reversed and automatically posts offsetting double-entry journal vouchers (`Dr = Cr`), preserving ledger integrity and audit trails.
- **Search History Persistence & Trigger**: Search terms are saved to the recent history list (capped at the last 5 unique terms, most recent first) when the user submits via the keyboard **Search / Done** key (or selects a quick match), with one-tap chips to re-apply a previous search, individual chip dismissal, and a **Clear** button.
- **Clean Window Headers**: Every screen and module window displays only its primary title without descriptive subtitle text underneath.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Recent Search History (Last 5 Terms)**: Displays interactive history chips directly inside the expanded top search bar panel so users can re-run frequent searches with a single tap.
  2. **1.5s Hold + Swipe-Down Batch Selection Mode**: Lets users hold any transaction for 1.5 seconds and drag down across rows to select multiple transactions at once, exposing a contextual bulk action bar for **Delete (Reverse)** and **Categorize**.
  3. **Minimalist Window Headers**: Strips out the secondary description lines beneath window titles across Home, Accounts, Activity, Investments, Statements, Analytics, More Hub, Loans, Budgets & Goals, Gift Cards, Subscriptions, Taxes, and Settings.
- **Target Audience / Persona**: Personal finance and accounting users managing high volumes of INR transactions on handheld screens (Google Pixel 4a viewport) who need fast filtering, rapid multi-row actions, and an uncluttered header hierarchy.
- **Key Value**: Eliminates repetitive typing in search, replaces tedious one-by-one transaction edits with fluid drag-selection bulk actions, and sharpens visual hierarchy across every window.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Top Search Bar & Last 5 Search History**:
     - Tapping the top-right **Search** button expands the search bar and focuses the input.
     - Pressing the keyboard **Search** or **Done** button commits the current non-blank query into the **Recent Searches (Max 5)** list (deduplicated, newest first).
     - Below the search field, a compact **Recent Searches** row displays up to 5 pill chips with a history icon and term label, plus a **Clear** action to wipe history. Tapping any chip immediately populates the search bar and filters results.
  2. **1.5-Second Hold & Swipe-Down Batch Transaction Selection**:
     - On transaction lists (Activity screen and Home recent transactions), holding a finger on a transaction card for **1.5 seconds (1,500ms)** triggers haptic/visual feedback, activates **Batch Selection Mode**, and selects that transaction.
     - Keeping the finger pressed and swiping downward (or upward) across the transaction list automatically selects each transaction row the pointer passes over.
     - Once Batch Selection Mode is active, a contextual **Batch Action Bar** appears at the top of the transaction list showing:
       - Selected count badge (e.g., `3 Selected`), **Select All**, and **Cancel (X)**.
       - **Categorize** button: Opens a single-tab `"Choose"` category selector dialog to update the category (and corresponding income/expense ledger mapping) for all selected transactions at once.
       - **Delete** button: Reverses all selected active transactions with offsetting double-entry journal entries and shows the floating 2-second notification pop-up.
     - Inline per-row `"Reverse"` buttons are removed from individual transaction cards so deletion/reversal is triggered cleanly through the 1.5-second hold gesture.
  3. **Simplified Window Headers**:
     - Every screen header (Transaction Activity, Financial Accounts, Investment Portfolio, Indian Financial Statements, Analytics & Financial Performance, Finance Vault Modules, Loans & Credit Cards, Budgets & Savings Goals, Gift Cards Vault, Subscriptions & Renewals, Indian Tax Records, Settings & Security) renders its title cleanly with no subtitle description beneath it.
- **Visual Identity & Theme**:
  - Preserves the **Obsidian & Emerald Vault** palette (`VaultObsidianBg`, `VaultCardSurface`, `VaultCardElevated`, `VaultEmerald`, `VaultCyan`, `VaultGold`, `VaultCoral`) with high-contrast selection borders (`VaultEmerald` highlight ring and check badge on selected transactions).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom 1,500ms Long-Press + Pointer Drag Gesture**
  - *Chosen Approach*: Use a custom pointer gesture detector (`awaitEachGesture` with a 1,500ms hold threshold before touch slop is exceeded, followed by continuous pointer Y-coordinate hit-testing against rendered item bounds in `LazyColumn`) rather than Android's default ~400ms `combinedClickable` long press.
  - *Why*: Meets the exact 1.5-second hold requirement and seamlessly transitions into vertical drag-to-select without lifting the finger.
- **Decision 2: Accounting-Safe Bulk Deletion & Bulk Categorization**
  - *Chosen Approach*: Bulk **Delete** executes atomic double-entry reversals (`AccountingEngine.buildReversalPlan`) for all selected non-reversed transactions in a single database transaction. Bulk **Categorize** updates each selected transaction's category and synchronizes its corresponding income/expense journal line ledger account so Trial Balance, P&L, and General Ledger remain 100% reconciled.
  - *Why*: Guarantees `Total Assets = Total Liabilities + Net Worth` and `Total Debits = Total Credits` after bulk operations.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         Top Search Bar & History                          │
│  [Search Input (ImeAction.Search/Done)] ──► Commits to Last 5 Queries     │
│  [Recent Searches Row (Up to 5 Chips)]  ──► Tap Chip = Filter Immediately │
│                                         ──► Tap "Clear" = Wipe History    │
└────────────────────────────────────┬──────────────────────────────────────┘
                                     │
                                     ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                 1.5s Hold + Swipe-Down Transaction List                   │
│  Pointer Down (1,500ms Timer) ──► Activates Batch Mode + Selects Item     │
│  Pointer Drag Down/Up         ──► Hit-Tests LazyListLayoutInfo Item Rows  │
│                                   & Adds Crossed Transaction IDs          │
└───────────────────┬───────────────────────────────────┬───────────────────┘
                    │                                   │
                    ▼                                   ▼
┌───────────────────────────────────────┐ ┌─────────────────────────────────┐
│     Bulk Delete (Reverse) Action      │ │     Bulk Categorize Action      │
│ • Atomic DB Transaction               │ │ • "Choose" Category Dialog      │
│ • Marks selected txs as Reversed      │ │ • Updates Transaction Category  │
│ • Posts offsetting Dr/Cr Journal      │ │ • Updates Category Ledger Line  │
│   Vouchers & Audit Logs               │ │   in Double-Entry Journal       │
└───────────────────────────────────────┘ └─────────────────────────────────┘
```

- **Data Model & State**:
  - **Search History**: Stored as a reactive list of up to 5 recent query strings in `FinanceVaultViewModel` (persisted via local preferences/state so it stays responsive across tabs), with `commitSearchQuery(query)`, `removeSearchHistoryItem(query)`, and `clearSearchHistory()`.
  - **Batch Selection State**: Managed in `ActivityScreen` / `HomeScreen` with `selectedTransactionIds: Set<Long>`, `isBatchSelectionMode: Boolean`, and a bulk category dialog using `VaultChooseSelector`.
  - **Repository Bulk Operations**:
    - `reverseTransactionsBulk(transactionIds: Set<Long>): Int`
    - `categorizeTransactionsBulk(transactionIds: Set<Long>, newCategory: String): Int`
