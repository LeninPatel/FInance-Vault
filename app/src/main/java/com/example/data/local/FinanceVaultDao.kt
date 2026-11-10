package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceVaultDao {

    // Settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    // Accounts
    @Query("SELECT * FROM accounts WHERE isDemo = :isDemo ORDER BY isArchived ASC, name ASC")
    fun observeAccounts(isDemo: Boolean): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getAccounts(isDemo: Boolean): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    // Ledger Accounts (Chart of Accounts)
    @Query("SELECT * FROM ledger_accounts WHERE isDemo = :isDemo ORDER BY code ASC")
    fun observeLedgerAccounts(isDemo: Boolean): Flow<List<LedgerAccountEntity>>

    @Query("SELECT * FROM ledger_accounts WHERE isDemo = :isDemo ORDER BY code ASC")
    suspend fun getLedgerAccounts(isDemo: Boolean): List<LedgerAccountEntity>

    @Query("SELECT * FROM ledger_accounts WHERE id = :id")
    suspend fun getLedgerAccountById(id: Long): LedgerAccountEntity?

    @Query("SELECT * FROM ledger_accounts WHERE linkedUserAccountId = :userAccountId AND isDemo = :isDemo LIMIT 1")
    suspend fun getLedgerByUserAccountId(userAccountId: Long, isDemo: Boolean): LedgerAccountEntity?

    @Query("SELECT * FROM ledger_accounts WHERE linkedInvestmentId = :investmentId AND isDemo = :isDemo LIMIT 1")
    suspend fun getLedgerByInvestmentId(investmentId: Long, isDemo: Boolean): LedgerAccountEntity?

    @Query("SELECT * FROM ledger_accounts WHERE linkedLoanId = :loanId AND isDemo = :isDemo LIMIT 1")
    suspend fun getLedgerByLoanId(loanId: Long, isDemo: Boolean): LedgerAccountEntity?

    @Query("SELECT * FROM ledger_accounts WHERE linkedGiftCardId = :giftCardId AND isDemo = :isDemo LIMIT 1")
    suspend fun getLedgerByGiftCardId(giftCardId: Long, isDemo: Boolean): LedgerAccountEntity?

    @Query("SELECT * FROM ledger_accounts WHERE classification = :classification AND isSystemAccount = 1 AND isDemo = :isDemo LIMIT 1")
    suspend fun getSystemLedgerByClassification(
        classification: LedgerClassification,
        isDemo: Boolean
    ): LedgerAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerAccount(ledgerAccount: LedgerAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerAccounts(ledgerAccounts: List<LedgerAccountEntity>)

    @Update
    suspend fun updateLedgerAccount(ledgerAccount: LedgerAccountEntity)

    // Transactions
    @Query("SELECT * FROM transactions WHERE isDemo = :isDemo AND transactionType != 'REVERSAL' ORDER BY dateEpochDay DESC, id DESC")
    fun observeTransactions(isDemo: Boolean): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDemo = :isDemo ORDER BY dateEpochDay ASC, id ASC")
    suspend fun getTransactions(isDemo: Boolean): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id IN (:ids)")
    suspend fun getTransactionsByIds(ids: List<Long>): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE isDemo = :isDemo")
    suspend fun getTransactionCount(isDemo: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    // Journal Entries & Lines
    @Query("SELECT * FROM journal_entries WHERE isDemo = :isDemo ORDER BY dateEpochDay ASC, id ASC")
    fun observeJournalEntries(isDemo: Boolean): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE isDemo = :isDemo ORDER BY dateEpochDay ASC, id ASC")
    suspend fun getJournalEntries(isDemo: Boolean): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getJournalEntryForTransaction(transactionId: Long): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Update
    suspend fun updateJournalEntry(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_lines WHERE isDemo = :isDemo ORDER BY dateEpochDay ASC, journalEntryId ASC, id ASC")
    fun observeJournalLines(isDemo: Boolean): Flow<List<JournalLineEntity>>

    @Query("SELECT * FROM journal_lines WHERE isDemo = :isDemo ORDER BY dateEpochDay ASC, journalEntryId ASC, id ASC")
    suspend fun getJournalLines(isDemo: Boolean): List<JournalLineEntity>

    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :journalEntryId ORDER BY id ASC")
    suspend fun getLinesForJournalEntry(journalEntryId: Long): List<JournalLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalLineEntity>)

    @Update
    suspend fun updateJournalLine(line: JournalLineEntity)

    // Investments
    @Query("SELECT * FROM investments WHERE isDemo = :isDemo ORDER BY purchaseDateEpochDay DESC, id DESC")
    fun observeInvestments(isDemo: Boolean): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getInvestments(isDemo: Boolean): List<InvestmentEntity>

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getInvestmentById(id: Long): InvestmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: InvestmentEntity): Long

    @Update
    suspend fun updateInvestment(investment: InvestmentEntity)

    // Loans & Loan Payments
    @Query("SELECT * FROM loans WHERE isDemo = :isDemo ORDER BY isClosed ASC, startDateEpochDay DESC")
    fun observeLoans(isDemo: Boolean): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getLoans(isDemo: Boolean): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("SELECT * FROM loan_payments WHERE isDemo = :isDemo ORDER BY dateEpochDay DESC, id DESC")
    fun observeLoanPayments(isDemo: Boolean): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getLoanPayments(isDemo: Boolean): List<LoanPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayment(payment: LoanPaymentEntity): Long

    // Budgets
    @Query("SELECT * FROM budgets WHERE isDemo = :isDemo ORDER BY id DESC")
    fun observeBudgets(isDemo: Boolean): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getBudgets(isDemo: Boolean): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    // Goals
    @Query("SELECT * FROM goals WHERE isDemo = :isDemo ORDER BY id DESC")
    fun observeGoals(isDemo: Boolean): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getGoals(isDemo: Boolean): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    // Gift Cards
    @Query("SELECT * FROM gift_cards WHERE isDemo = :isDemo ORDER BY id DESC")
    fun observeGiftCards(isDemo: Boolean): Flow<List<GiftCardEntity>>

    @Query("SELECT * FROM gift_cards WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getGiftCards(isDemo: Boolean): List<GiftCardEntity>

    @Query("SELECT * FROM gift_cards WHERE id = :id")
    suspend fun getGiftCardById(id: Long): GiftCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGiftCard(giftCard: GiftCardEntity): Long

    @Update
    suspend fun updateGiftCard(giftCard: GiftCardEntity)

    // Subscriptions
    @Query("SELECT * FROM subscriptions WHERE isDemo = :isDemo ORDER BY nextRenewalEpochDay ASC")
    fun observeSubscriptions(isDemo: Boolean): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getSubscriptions(isDemo: Boolean): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: Long): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Long)

    // Taxes
    @Query("SELECT * FROM taxes WHERE isDemo = :isDemo ORDER BY paymentDateEpochDay DESC, id DESC")
    fun observeTaxRecords(isDemo: Boolean): Flow<List<TaxRecordEntity>>

    @Query("SELECT * FROM taxes WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getTaxRecords(isDemo: Boolean): List<TaxRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxRecord(taxRecord: TaxRecordEntity): Long

    // Audit Logs
    @Query(
        "SELECT * FROM audit_logs WHERE isDemo = :isDemo AND action IN (" +
            "'VOUCHER_POSTED', 'VOUCHER_REVERSED', 'GOAL_CONTRIBUTION', 'INVESTMENT_VALUATION_UPDATED', 'TAX_RECORDED'" +
            ") ORDER BY timestampMillis DESC, id DESC"
    )
    fun observeAuditLogs(isDemo: Boolean): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE isDemo = :isDemo ORDER BY id ASC")
    suspend fun getAuditLogs(isDemo: Boolean): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    // Scoped deletion by mode (real vs demo)
    @Query("DELETE FROM accounts WHERE isDemo = :isDemo")
    suspend fun clearAccountsByMode(isDemo: Boolean)

    @Query("DELETE FROM ledger_accounts WHERE isDemo = :isDemo")
    suspend fun clearLedgerAccountsByMode(isDemo: Boolean)

    @Query("DELETE FROM transactions WHERE isDemo = :isDemo")
    suspend fun clearTransactionsByMode(isDemo: Boolean)

    @Query("DELETE FROM journal_entries WHERE isDemo = :isDemo")
    suspend fun clearJournalEntriesByMode(isDemo: Boolean)

    @Query("DELETE FROM journal_lines WHERE isDemo = :isDemo")
    suspend fun clearJournalLinesByMode(isDemo: Boolean)

    @Query("DELETE FROM investments WHERE isDemo = :isDemo")
    suspend fun clearInvestmentsByMode(isDemo: Boolean)

    @Query("DELETE FROM loans WHERE isDemo = :isDemo")
    suspend fun clearLoansByMode(isDemo: Boolean)

    @Query("DELETE FROM loan_payments WHERE isDemo = :isDemo")
    suspend fun clearLoanPaymentsByMode(isDemo: Boolean)

    @Query("DELETE FROM budgets WHERE isDemo = :isDemo")
    suspend fun clearBudgetsByMode(isDemo: Boolean)

    @Query("DELETE FROM goals WHERE isDemo = :isDemo")
    suspend fun clearGoalsByMode(isDemo: Boolean)

    @Query("DELETE FROM gift_cards WHERE isDemo = :isDemo")
    suspend fun clearGiftCardsByMode(isDemo: Boolean)

    @Query("DELETE FROM subscriptions WHERE isDemo = :isDemo")
    suspend fun clearSubscriptionsByMode(isDemo: Boolean)

    @Query("DELETE FROM taxes WHERE isDemo = :isDemo")
    suspend fun clearTaxesByMode(isDemo: Boolean)

    @Query("DELETE FROM audit_logs WHERE isDemo = :isDemo")
    suspend fun clearAuditLogsByMode(isDemo: Boolean)
}
