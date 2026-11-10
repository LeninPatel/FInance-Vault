package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class FinanceTypeConverters {
    @TypeConverter
    fun fromAccountType(v: AccountType): String = v.name
    @TypeConverter
    fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter
    fun fromNormalBalance(v: NormalBalance): String = v.name
    @TypeConverter
    fun toNormalBalance(v: String): NormalBalance = NormalBalance.valueOf(v)

    @TypeConverter
    fun fromLedgerClassification(v: LedgerClassification): String = v.name
    @TypeConverter
    fun toLedgerClassification(v: String): LedgerClassification = LedgerClassification.valueOf(v)

    @TypeConverter
    fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter
    fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter
    fun fromPostingStatus(v: PostingStatus): String = v.name
    @TypeConverter
    fun toPostingStatus(v: String): PostingStatus = PostingStatus.valueOf(v)

    @TypeConverter
    fun fromInvestmentCategory(v: InvestmentCategory): String = v.name
    @TypeConverter
    fun toInvestmentCategory(v: String): InvestmentCategory = InvestmentCategory.valueOf(v)

    @TypeConverter
    fun fromCompoundingFrequency(v: CompoundingFrequency): String = v.name
    @TypeConverter
    fun toCompoundingFrequency(v: String): CompoundingFrequency = CompoundingFrequency.valueOf(v)

    @TypeConverter
    fun fromLoanType(v: LoanType): String = v.name
    @TypeConverter
    fun toLoanType(v: String): LoanType = LoanType.valueOf(v)

    @TypeConverter
    fun fromBudgetPeriod(v: BudgetPeriod): String = v.name
    @TypeConverter
    fun toBudgetPeriod(v: String): BudgetPeriod = BudgetPeriod.valueOf(v)

    @TypeConverter
    fun fromGoalType(v: GoalType): String = v.name
    @TypeConverter
    fun toGoalType(v: String): GoalType = GoalType.valueOf(v)

    @TypeConverter
    fun fromBillingFrequency(v: BillingFrequency): String = v.name
    @TypeConverter
    fun toBillingFrequency(v: String): BillingFrequency =
        runCatching { BillingFrequency.valueOf(v) }.getOrDefault(BillingFrequency.MONTHLY)

    @TypeConverter
    fun fromTaxType(v: TaxType): String = v.name
    @TypeConverter
    fun toTaxType(v: String): TaxType = TaxType.valueOf(v)

    @TypeConverter
    fun fromThemeMode(v: ThemeMode): String = v.name
    @TypeConverter
    fun toThemeMode(v: String): ThemeMode = runCatching { ThemeMode.valueOf(v) }.getOrDefault(ThemeMode.DARK)
}

@Database(
    entities = [
        AppSettingsEntity::class,
        AccountEntity::class,
        LedgerAccountEntity::class,
        TransactionEntity::class,
        JournalEntryEntity::class,
        JournalLineEntity::class,
        InvestmentEntity::class,
        LoanEntity::class,
        LoanPaymentEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        GiftCardEntity::class,
        SubscriptionEntity::class,
        TaxRecordEntity::class,
        AuditLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(FinanceTypeConverters::class)
abstract class FinanceVaultDatabase : RoomDatabase() {
    abstract fun dao(): FinanceVaultDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceVaultDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN isBiometricEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'DARK'")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN locationName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN longitude REAL")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN customCategoriesJson TEXT NOT NULL DEFAULT '{}'")
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_isDemo_isArchived_name` ON `accounts` (`isDemo`, `isArchived`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_accounts_isDemo_code` ON `ledger_accounts` (`isDemo`, `code`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_accounts_isDemo_classification_isSystemAccount` ON `ledger_accounts` (`isDemo`, `classification`, `isSystemAccount`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_isDemo_dateEpochDay_id` ON `transactions` (`isDemo`, `dateEpochDay`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_isDemo_transactionType_dateEpochDay` ON `transactions` (`isDemo`, `transactionType`, `dateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_isDemo_dateEpochDay_id` ON `journal_entries` (`isDemo`, `dateEpochDay`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_lines_isDemo_dateEpochDay_journalEntryId_id` ON `journal_lines` (`isDemo`, `dateEpochDay`, `journalEntryId`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_lines_isDemo_ledgerAccountId` ON `journal_lines` (`isDemo`, `ledgerAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investments_isDemo_purchaseDateEpochDay_id` ON `investments` (`isDemo`, `purchaseDateEpochDay`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loans_isDemo_isClosed_startDateEpochDay` ON `loans` (`isDemo`, `isClosed`, `startDateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_payments_isDemo_dateEpochDay_id` ON `loan_payments` (`isDemo`, `dateEpochDay`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_isDemo_nextRenewalEpochDay` ON `subscriptions` (`isDemo`, `nextRenewalEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_taxes_isDemo_paymentDateEpochDay_id` ON `taxes` (`isDemo`, `paymentDateEpochDay`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_logs_isDemo_timestampMillis_id` ON `audit_logs` (`isDemo`, `timestampMillis`, `id`)")
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN customIntervalCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN customIntervalUnit TEXT NOT NULL DEFAULT 'MONTHS'")
            }
        }

        fun getInstance(context: Context): FinanceVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceVaultDatabase::class.java,
                    "finance_vault.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
