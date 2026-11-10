package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val panMasked: String = "",
    val activeFinancialYear: String = "FY 2026-27",
    val isPrivacyModeEnabled: Boolean = false,
    val isDemoModeEnabled: Boolean = false,
    val pinHashSha256: String? = null,
    val pinSalt: String? = null,
    val isBiometricEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val autoLockMinutes: Int = 5,
    val customCategoriesJson: String = "{}",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "accounts",
    indices = [
        Index("accountType"),
        Index("isDemo"),
        Index(value = ["isDemo", "isArchived", "name"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accountType: AccountType,
    val bankName: String = "",
    val branchName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val accountHolder: String = "",
    val upiId: String = "",
    val openingBalancePaise: Long = 0L,
    val isOpeningBalanceRecorded: Boolean = false,
    val openingDateEpochDay: Long,
    val creditLimitPaise: Long? = null,
    val billingDayOfMonth: Int? = null,
    val notes: String = "",
    val isArchived: Boolean = false,
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "ledger_accounts",
    indices = [
        Index("code"),
        Index("linkedUserAccountId"),
        Index("linkedInvestmentId"),
        Index("linkedLoanId"),
        Index("isDemo"),
        Index(value = ["isDemo", "code"]),
        Index(value = ["isDemo", "classification", "isSystemAccount"])
    ]
)
data class LedgerAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val classification: LedgerClassification,
    val normalBalance: NormalBalance,
    val linkedUserAccountId: Long? = null,
    val linkedInvestmentId: Long? = null,
    val linkedLoanId: Long? = null,
    val linkedGiftCardId: Long? = null,
    val isSystemAccount: Boolean = false,
    val isDemo: Boolean = false
)

@Immutable
@Entity(
    tableName = "transactions",
    indices = [
        Index("dateEpochDay"),
        Index("transactionType"),
        Index("sourceAccountId"),
        Index("destinationAccountId"),
        Index("isDemo"),
        Index(value = ["isDemo", "dateEpochDay", "id"]),
        Index(value = ["isDemo", "transactionType", "dateEpochDay"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherNumber: String,
    val dateEpochDay: Long,
    val amountPaise: Long,
    val principalComponentPaise: Long = 0L,
    val interestComponentPaise: Long = 0L,
    val chargesPaise: Long = 0L,
    val costBasisDisposedPaise: Long = 0L,
    val transactionType: TransactionType,
    val sourceAccountId: Long? = null,
    val destinationAccountId: Long? = null,
    val category: String,
    val description: String,
    val referenceNumber: String = "",
    val notes: String = "",
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val attachmentName: String = "",
    val linkedInvestmentId: Long? = null,
    val linkedLoanId: Long? = null,
    val linkedGiftCardId: Long? = null,
    val linkedTaxRecordId: Long? = null,
    val linkedOriginalTransactionId: Long? = null,
    val isReversed: Boolean = false,
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "journal_entries",
    indices = [
        Index("transactionId"),
        Index("dateEpochDay"),
        Index("voucherNumber"),
        Index("isDemo"),
        Index(value = ["isDemo", "dateEpochDay", "id"])
    ]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val voucherNumber: String,
    val dateEpochDay: Long,
    val narration: String,
    val financialYear: String,
    val postingStatus: PostingStatus = PostingStatus.POSTED,
    val isReversal: Boolean = false,
    val reversedJournalEntryId: Long? = null,
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "journal_lines",
    indices = [
        Index("journalEntryId"),
        Index("transactionId"),
        Index("ledgerAccountId"),
        Index("dateEpochDay"),
        Index("isDemo"),
        Index(value = ["isDemo", "dateEpochDay", "journalEntryId", "id"]),
        Index(value = ["isDemo", "ledgerAccountId"])
    ]
)
data class JournalLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journalEntryId: Long,
    val transactionId: Long,
    val voucherNumber: String,
    val dateEpochDay: Long,
    val ledgerAccountId: Long,
    val ledgerAccountName: String,
    val ledgerFolio: String,
    val debitPaise: Long,
    val creditPaise: Long,
    val lineNarration: String,
    val isDemo: Boolean = false
)

@Immutable
@Entity(
    tableName = "investments",
    indices = [
        Index("category"),
        Index("isDemo"),
        Index(value = ["isDemo", "purchaseDateEpochDay", "id"])
    ]
)
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: InvestmentCategory,
    val purchaseDateEpochDay: Long,
    val quantity: Double = 1.0,
    val units: Double = 1.0,
    val purchasePricePerUnitPaise: Long = 0L,
    val investedAmountPaise: Long,
    val chargesPaise: Long = 0L,
    val currentRecordedValuePaise: Long? = null,
    val isCurrentValueRecorded: Boolean = false,
    val realizedProfitLossPaise: Long = 0L,
    val interestEarnedPaise: Long = 0L,
    val dividendsReceivedPaise: Long = 0L,
    val linkedPaymentAccountId: Long? = null,
    val maturityDateEpochDay: Long? = null,
    val annualInterestRatePercent: Double? = null,
    val compoundingFrequency: CompoundingFrequency = CompoundingFrequency.QUARTERLY,
    val durationMonths: Int? = null,
    val notes: String = "",
    val isFullyRedeemed: Boolean = false,
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "loans",
    indices = [
        Index("loanType"),
        Index("isDemo"),
        Index(value = ["isDemo", "isClosed", "startDateEpochDay"])
    ]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val loanType: LoanType,
    val lenderName: String = "",
    val principalPaise: Long,
    val remainingPrincipalPaise: Long,
    val annualInterestRatePercent: Double,
    val tenureMonths: Int,
    val remainingInstallments: Int,
    val startDateEpochDay: Long,
    val emiAmountPaise: Long,
    val totalInterestPaidPaise: Long = 0L,
    val linkedBankAccountId: Long? = null,
    val notes: String = "",
    val isClosed: Boolean = false,
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "loan_payments",
    indices = [
        Index("loanId"),
        Index("transactionId"),
        Index("isDemo"),
        Index(value = ["isDemo", "dateEpochDay", "id"])
    ]
)
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val transactionId: Long,
    val dateEpochDay: Long,
    val totalPaymentPaise: Long,
    val principalComponentPaise: Long,
    val interestComponentPaise: Long,
    val remainingPrincipalAfterPaise: Long,
    val isDemo: Boolean = false
)

@Immutable
@Entity(
    tableName = "budgets",
    indices = [Index("category"), Index("isDemo")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val budgetPeriod: BudgetPeriod,
    val category: String,
    val limitPaise: Long,
    val financialYear: String,
    val notes: String = "",
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "goals",
    indices = [Index("goalType"), Index("isDemo")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val goalType: GoalType,
    val targetAmountPaise: Long,
    val currentSavedPaise: Long = 0L,
    val targetDateEpochDay: Long? = null,
    val linkedAccountId: Long? = null,
    val notes: String = "",
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "gift_cards",
    indices = [Index("brand"), Index("isDemo")]
)
data class GiftCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brand: String,
    val name: String,
    val initialValuePaise: Long,
    val remainingValuePaise: Long,
    val purchaseDateEpochDay: Long,
    val expirationDateEpochDay: Long? = null,
    val cardNumberMasked: String = "",
    val redemptionCodeProtected: String = "",
    val notes: String = "",
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "subscriptions",
    indices = [
        Index("isActive"),
        Index("isDemo"),
        Index(value = ["isDemo", "nextRenewalEpochDay"])
    ]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val costPaise: Long,
    val billingFrequency: BillingFrequency,
    val nextRenewalEpochDay: Long,
    val paymentAccountId: Long? = null,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val notes: String = "",
    val isActive: Boolean = true,
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val customIntervalCount: Int = 1,
    val customIntervalUnit: String = "MONTHS"
) {
    fun effectiveMonthsMultiplier(): Double {
        return when (billingFrequency) {
            BillingFrequency.CUSTOM -> {
                val count = customIntervalCount.coerceAtLeast(1)
                if (customIntervalUnit.equals("DAYS", ignoreCase = true)) {
                    (count / 30.0).coerceAtLeast(0.1)
                } else {
                    count.toDouble()
                }
            }
            else -> billingFrequency.monthsMultiplier
        }
    }

    fun cycleLabel(): String {
        return when (billingFrequency) {
            BillingFrequency.CUSTOM -> {
                val count = customIntervalCount.coerceAtLeast(1)
                if (customIntervalUnit.equals("DAYS", ignoreCase = true)) {
                    if (count == 1) "Every Day" else "Every $count Days"
                } else {
                    if (count == 1) "Every Month" else "Every $count Months"
                }
            }
            BillingFrequency.BIMONTHLY -> "Every 2 Months"
            BillingFrequency.FOUR_MONTHLY -> "Every 4 Months"
            else -> billingFrequency.displayName
        }
    }
}

@Immutable
@Entity(
    tableName = "taxes",
    indices = [
        Index("financialYear"),
        Index("taxType"),
        Index("isDemo"),
        Index(value = ["isDemo", "paymentDateEpochDay", "id"])
    ]
)
data class TaxRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taxType: TaxType,
    val financialYear: String,
    val assessmentYear: String,
    val amountPaidPaise: Long,
    val dueDateEpochDay: Long? = null,
    val paymentDateEpochDay: Long,
    val challanNumber: String = "",
    val paymentAccountId: Long? = null,
    val linkedTransactionId: Long? = null,
    val supportingDocLabel: String = "",
    val notes: String = "",
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "audit_logs",
    indices = [
        Index("timestampMillis"),
        Index("isDemo"),
        Index(value = ["isDemo", "timestampMillis", "id"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val action: String,
    val entityType: String,
    val entityId: Long,
    val summary: String,
    val isDemo: Boolean = false
)
