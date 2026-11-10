package com.example.data.local

enum class AccountType(
    val displayName: String,
    val ledgerClassification: LedgerClassification,
    val normalBalance: NormalBalance
) {
    CASH("Cash Account", LedgerClassification.ASSET_CURRENT_CASH, NormalBalance.DEBIT),
    SAVINGS("Savings Bank Account", LedgerClassification.ASSET_CURRENT_BANK, NormalBalance.DEBIT),
    CURRENT("Current Bank Account", LedgerClassification.ASSET_CURRENT_BANK, NormalBalance.DEBIT),
    BANK("Bank Account", LedgerClassification.ASSET_CURRENT_BANK, NormalBalance.DEBIT),
    DIGITAL_WALLET("Digital Wallet", LedgerClassification.ASSET_CURRENT_CASH, NormalBalance.DEBIT),
    UPI("UPI-Linked Account", LedgerClassification.ASSET_CURRENT_BANK, NormalBalance.DEBIT),
    CREDIT_CARD("Credit Card", LedgerClassification.LIABILITY_CURRENT_CREDIT_CARD, NormalBalance.CREDIT),
    OTHER("Other", LedgerClassification.ASSET_CURRENT_OTHER, NormalBalance.DEBIT)
}

enum class NormalBalance {
    DEBIT,
    CREDIT
}

enum class LedgerClassification(
    val displayTitle: String,
    val normalBalance: NormalBalance,
    val folioPrefix: String
) {
    // Non-Current Assets
    ASSET_NON_CURRENT_INVESTMENT("Non-Current Assets: Investments", NormalBalance.DEBIT, "LF-INV"),
    ASSET_NON_CURRENT_PROPERTY("Non-Current Assets: Property & Long-Term Assets", NormalBalance.DEBIT, "LF-PRP"),

    // Current Assets
    ASSET_CURRENT_CASH("Current Assets: Cash and Cash Equivalents", NormalBalance.DEBIT, "LF-CSH"),
    ASSET_CURRENT_BANK("Current Assets: Bank Balances", NormalBalance.DEBIT, "LF-BNK"),
    ASSET_CURRENT_OTHER("Current Assets: Other Current Assets", NormalBalance.DEBIT, "LF-OCA"),

    // Non-Current Liabilities
    LIABILITY_NON_CURRENT_LOAN("Non-Current Liabilities: Loans and Borrowings", NormalBalance.CREDIT, "LF-LON"),

    // Current Liabilities
    LIABILITY_CURRENT_CREDIT_CARD("Current Liabilities: Credit Card Outstanding", NormalBalance.CREDIT, "LF-CCD"),
    LIABILITY_CURRENT_PAYABLE("Current Liabilities: Other Payables", NormalBalance.CREDIT, "LF-PAY"),

    // Personal Net Worth / Equity
    EQUITY_OPENING_CAPITAL("Personal Net Worth: Opening Capital", NormalBalance.CREDIT, "LF-CAP"),
    EQUITY_CONTRIBUTION("Personal Net Worth: Additional Contributions", NormalBalance.CREDIT, "LF-CNT"),
    EQUITY_DRAWINGS("Personal Net Worth: Drawings / Adjustments", NormalBalance.DEBIT, "LF-DRW"),

    // Income
    INCOME_SALARY("Income: Salary Income", NormalBalance.CREDIT, "LF-INC-SAL"),
    INCOME_BUSINESS("Income: Business & Profession Income", NormalBalance.CREDIT, "LF-INC-BUS"),
    INCOME_INTEREST("Income: Interest Income", NormalBalance.CREDIT, "LF-INC-INT"),
    INCOME_DIVIDEND("Income: Dividend Income", NormalBalance.CREDIT, "LF-INC-DIV"),
    INCOME_CAPITAL_GAIN("Income: Realized Investment Gain", NormalBalance.CREDIT, "LF-INC-CGN"),
    INCOME_OTHER("Income: Other Income", NormalBalance.CREDIT, "LF-INC-OTH"),

    // Expenses
    EXPENSE_HOUSEHOLD("Expenses: Household Expenses", NormalBalance.DEBIT, "LF-EXP-HSH"),
    EXPENSE_EDUCATION("Expenses: Education Expenses", NormalBalance.DEBIT, "LF-EXP-EDU"),
    EXPENSE_TRANSPORT("Expenses: Transportation Expenses", NormalBalance.DEBIT, "LF-EXP-TRN"),
    EXPENSE_UTILITIES("Expenses: Utilities & Bills", NormalBalance.DEBIT, "LF-EXP-UTL"),
    EXPENSE_HEALTHCARE("Expenses: Healthcare & Medical", NormalBalance.DEBIT, "LF-EXP-HLT"),
    EXPENSE_INTEREST("Expenses: Interest Expenses", NormalBalance.DEBIT, "LF-EXP-INT"),
    EXPENSE_BANK_CHARGES("Expenses: Bank Charges & Fees", NormalBalance.DEBIT, "LF-EXP-BCH"),
    EXPENSE_INVESTMENT_FEES("Expenses: Brokerage & Investment Charges", NormalBalance.DEBIT, "LF-EXP-BRK"),
    EXPENSE_TAXES("Expenses: Taxes and Statutory Payments", NormalBalance.DEBIT, "LF-EXP-TAX"),
    EXPENSE_CAPITAL_LOSS("Expenses: Realized Investment Loss", NormalBalance.DEBIT, "LF-EXP-CLS"),
    EXPENSE_OTHER("Expenses: Other Expenses", NormalBalance.DEBIT, "LF-EXP-OTH");

    val isAsset: Boolean
        get() = name.startsWith("ASSET_")

    val isLiability: Boolean
        get() = name.startsWith("LIABILITY_")

    val isEquity: Boolean
        get() = name.startsWith("EQUITY_")

    val isIncome: Boolean
        get() = name.startsWith("INCOME_")

    val isExpense: Boolean
        get() = name.startsWith("EXPENSE_")
}

enum class TransactionType(
    val userLabel: String,
    val descriptionHint: String
) {
    OPENING_BALANCE("Opening Amount", "Initial balance recorded for account"),
    INCOME("Money In (Income)", "Salary, freelance, business, or other income"),
    EXPENSE("Money Out (Expense)", "Household, utilities, food, healthcare, etc."),
    TRANSFER("Account Transfer", "Transfer between your own bank/cash/wallet accounts"),
    CASH_DEPOSIT("Cash Deposit to Bank", "Deposit physical cash into a bank account"),
    CASH_WITHDRAWAL("Cash Withdrawal from Bank", "Withdraw cash from ATM / bank to cash in hand"),
    CREDIT_CARD_PURCHASE("Credit Card Spend", "Purchase made using a credit card"),
    CREDIT_CARD_REPAYMENT("Credit Card Bill Payment", "Pay off credit card dues from bank account"),
    INVESTMENT_PURCHASE("Investment Purchase", "Buy shares, mutual funds, FD, gold, bonds"),
    INVESTMENT_SALE("Investment Sale / Redemption", "Sell or redeem an investment with cost disposal"),
    LOAN_RECEIVED("Loan Received", "Loan disbursement credited to your account"),
    LOAN_REPAYMENT("Loan EMI / Repayment", "Repay loan split into principal and interest"),
    INTEREST_RECEIVED("Interest Received", "Savings, FD, RD, or bond interest received"),
    DIVIDEND_RECEIVED("Dividend Received", "Dividend credited to your bank account"),
    REFUND_RECEIVED("Purchase Refund", "Refund received reducing original expense"),
    BANK_CHARGE("Bank Charges / Fee", "SMS fee, maintenance fee, or penalty"),
    GIFT_CARD_PURCHASE("Gift Card Purchase", ""),
    GIFT_CARD_USAGE("Gift Card Spend", ""),
    TAX_PAYMENT("Tax Payment", ""),
    FD_MATURITY("Fixed Deposit Maturity", ""),
    RD_CONTRIBUTION("Recurring Deposit Installment", ""),
    RD_MATURITY("Recurring Deposit Maturity", ""),
    REVERSAL("Transaction Reversal", ""),
    CORRECTION("Adjustment / Correction", "")
}

enum class PostingStatus {
    POSTED,
    REVERSED
}

enum class InvestmentCategory(val displayName: String) {
    EQUITY_SHARES("Equity Shares"),
    ETF("Exchange-Traded Funds (ETF)"),
    MUTUAL_FUND("Mutual Funds"),
    SIP("Systematic Investment Plan (SIP)"),
    FIXED_DEPOSIT("Fixed Deposit (FD)"),
    RECURRING_DEPOSIT("Recurring Deposit (RD)"),
    BONDS("Bonds & Debentures"),
    GOVT_SECURITIES("Government Securities (G-Sec / PPF / SGB)"),
    GOLD("Gold"),
    SILVER("Silver"),
    OTHER("Other")
}

enum class CompoundingFrequency(val displayName: String, val periodsPerYear: Int) {
    SIMPLE("Simple Interest (No Compounding)", 0),
    MONTHLY("Monthly (12x / year)", 12),
    QUARTERLY("Quarterly (4x / year)", 4),
    HALF_YEARLY("Half-Yearly (2x / year)", 2),
    ANNUALLY("Annually (1x / year)", 1)
}

enum class LoanType(val displayName: String) {
    PERSONAL("Personal Loan"),
    EDUCATION("Education Loan"),
    HOME("Home / Property Loan"),
    VEHICLE("Vehicle Loan"),
    OTHER("Other")
}

enum class BudgetPeriod(val displayName: String) {
    MONTHLY("Monthly Budget"),
    ANNUAL("Annual Budget (FY)")
}

enum class GoalType(val displayName: String) {
    SAVINGS("Savings Goal"),
    EMERGENCY_FUND("Emergency Fund"),
    PURCHASE("Target Purchase")
}

enum class BillingFrequency(val displayName: String, val monthsMultiplier: Double) {
    MONTHLY("Monthly (Every 1 Month)", 1.0),
    BIMONTHLY("Every 2 Months", 2.0),
    QUARTERLY("Quarterly (Every 3 Months)", 3.0),
    FOUR_MONTHLY("Every 4 Months", 4.0),
    HALF_YEARLY("Half-Yearly (Every 6 Months)", 6.0),
    ANNUALLY("Annually (Every 12 Months)", 12.0),
    CUSTOM("Custom Interval / Date", 1.0)
}

enum class TaxType(val displayName: String) {
    INCOME_TAX("Income Tax"),
    TDS("Tax Deducted at Source (TDS)"),
    ADVANCE_TAX("Advance Tax"),
    SELF_ASSESSMENT_TAX("Self-Assessment Tax"),
    GST("Goods & Services Tax (GST)"),
    OTHER("Other")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}
