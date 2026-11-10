package com.example.domain.ai

import androidx.compose.runtime.Immutable
import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import java.util.Locale

@Immutable
data class LearnedTransactionPattern(
    val inferredCategory: String,
    val inferredTransactionType: TransactionType,
    val inferredLocationName: String,
    val preferredSourceAccount: AccountEntity? = null,
    val preferredDestinationAccount: AccountEntity? = null,
    val isAccountAutoSelectedFromHistory: Boolean = false,
    val isCategoryAutoSelectedFromHistory: Boolean = false,
    val requiresAccountDisambiguation: Boolean = false,
    val candidateAccounts: List<AccountEntity> = emptyList(),
    val patternExplanation: String = ""
)

/**
 * Analyzes historical transactions across varying amounts and timestamps to:
 * 1. Automatically infer the user's preferred Category, Account, and Location when a similar
 *    transaction (same merchant, keyword, or category) happened previously at different amounts/times.
 * 2. Detect when a transaction has multiple valid account options with no single dominant historical
 *    choice, prompting the user to select which option to use.
 */
object TransactionPatternAnalyzer {

    private val stopWords = setOf(
        "spent", "paid", "bought", "received", "earned", "got", "credited", "debited",
        "rupees", "rs", "inr", "for", "on", "at", "in", "from", "to", "into", "using",
        "via", "with", "by", "the", "a", "an", "my", "today", "yesterday", "transaction",
        "record", "add", "log", "voice", "expense", "income", "transfer", "amount"
    )

    fun analyzeForTransaction(
        utterance: String,
        fallbackType: TransactionType,
        fallbackCategory: String,
        explicitLocationName: String,
        explicitSourceAccountHint: String,
        explicitDestinationAccountHint: String,
        transactions: List<TransactionEntity>,
        activeAccounts: List<AccountEntity>
    ): LearnedTransactionPattern {
        val validHistory = transactions.filter { !it.isReversed }
        val accountById = activeAccounts.associateBy { it.id }

        // 1. Check if the user explicitly named an account in this utterance
        val explicitSrcAcc = matchExplicitAccount(explicitSourceAccountHint, activeAccounts)
        val explicitDstAcc = matchExplicitAccount(explicitDestinationAccountHint, activeAccounts)

        // 2. Extract meaningful content keywords from the utterance (ignoring numbers and stop words)
        val keywords = extractKeywords(utterance)
        val lowerUtterance = utterance.lowercase(Locale.US)

        // 3. Find historical transactions matching merchant/location or narration keywords (across any amount or time)
        val keywordMatchedTxs = validHistory.filter { tx ->
            val locMatch = explicitLocationName.isNotBlank() &&
                tx.locationName.equals(explicitLocationName, ignoreCase = true)
            val existingLocInUtterance = tx.locationName.isNotBlank() &&
                tx.locationName.length >= 3 &&
                lowerUtterance.contains(tx.locationName.lowercase(Locale.US))
            val descTokens = extractKeywords(tx.description)
            val sharedKeywords = keywords.intersect(descTokens)
            locMatch || existingLocInUtterance || sharedKeywords.isNotEmpty()
        }

        // Determine Category & TransactionType from history if strong keyword/merchant match exists
        var resolvedCategory = fallbackCategory
        var resolvedType = fallbackType
        var resolvedLocation = explicitLocationName
        var categoryLearnedFromHistory = false

        if (keywordMatchedTxs.isNotEmpty()) {
            // Weight more recent transactions slightly higher while counting frequency across different times/amounts
            val categoryScores = mutableMapOf<String, Double>()
            val typeScores = mutableMapOf<TransactionType, Double>()
            val locationScores = mutableMapOf<String, Double>()

            keywordMatchedTxs.forEachIndexed { idx, tx ->
                val recencyWeight = 1.0 + (1.0 / (idx + 2.0))
                if (tx.category.isNotBlank()) {
                    categoryScores[tx.category] = (categoryScores[tx.category] ?: 0.0) + recencyWeight
                }
                typeScores[tx.transactionType] = (typeScores[tx.transactionType] ?: 0.0) + recencyWeight
                if (tx.locationName.isNotBlank()) {
                    locationScores[tx.locationName] = (locationScores[tx.locationName] ?: 0.0) + recencyWeight
                }
            }

            val topCategory = categoryScores.maxByOrNull { it.value }?.key
            if (!topCategory.isNullOrBlank()) {
                resolvedCategory = topCategory
                categoryLearnedFromHistory = true
            }

            val topType = typeScores.maxByOrNull { it.value }?.key
            if (topType != null && (topType == TransactionType.EXPENSE || topType == TransactionType.INCOME)) {
                resolvedType = topType
            }

            if (resolvedLocation.isBlank()) {
                resolvedLocation = locationScores.maxByOrNull { it.value }?.key.orEmpty()
            }
        }

        // 4. Now analyze all historical transactions in the resolvedCategory (and keywordMatchedTxs)
        //    to see if the user consistently uses a specific Account for this category/merchant across different amounts & times!
        val categoryMatchedTxs = validHistory.filter { tx ->
            tx.transactionType == resolvedType &&
                tx.category.equals(resolvedCategory, ignoreCase = true)
        }

        val combinedRelevantTxs = (keywordMatchedTxs + categoryMatchedTxs).distinctBy { it.id }

        // If user explicitly specified the required account(s) in their voice command, no disambiguation is needed
        if (resolvedType == TransactionType.INCOME && explicitDstAcc != null) {
            return LearnedTransactionPattern(
                inferredCategory = resolvedCategory,
                inferredTransactionType = resolvedType,
                inferredLocationName = resolvedLocation,
                preferredDestinationAccount = explicitDstAcc,
                isAccountAutoSelectedFromHistory = false,
                isCategoryAutoSelectedFromHistory = categoryLearnedFromHistory
            )
        }
        if (resolvedType == TransactionType.EXPENSE && explicitSrcAcc != null) {
            return LearnedTransactionPattern(
                inferredCategory = resolvedCategory,
                inferredTransactionType = resolvedType,
                inferredLocationName = resolvedLocation,
                preferredSourceAccount = explicitSrcAcc,
                isAccountAutoSelectedFromHistory = false,
                isCategoryAutoSelectedFromHistory = categoryLearnedFromHistory
            )
        }
        if (resolvedType == TransactionType.TRANSFER && explicitSrcAcc != null && explicitDstAcc != null) {
            return LearnedTransactionPattern(
                inferredCategory = resolvedCategory,
                inferredTransactionType = resolvedType,
                inferredLocationName = resolvedLocation,
                preferredSourceAccount = explicitSrcAcc,
                preferredDestinationAccount = explicitDstAcc,
                isAccountAutoSelectedFromHistory = false,
                isCategoryAutoSelectedFromHistory = categoryLearnedFromHistory
            )
        }

        // Score accounts used historically for this merchant / category across different amounts and dates
        val accountWeights = mutableMapOf<Long, Double>()
        val accountCounts = mutableMapOf<Long, Int>()

        // Give 2x weight to exact merchant/keyword matches and 1x weight to same-category matches
        for ((index, tx) in combinedRelevantTxs.withIndex()) {
            val accId = if (resolvedType == TransactionType.INCOME) {
                tx.destinationAccountId ?: tx.sourceAccountId
            } else {
                tx.sourceAccountId ?: tx.destinationAccountId
            } ?: continue

            if (!accountById.containsKey(accId)) continue
            val isKeywordMatch = keywordMatchedTxs.any { it.id == tx.id }
            val baseWeight = if (isKeywordMatch) 2.0 else 1.0
            val recencyBonus = 0.5 / (index + 1.0)
            accountWeights[accId] = (accountWeights[accId] ?: 0.0) + baseWeight + recencyBonus
            accountCounts[accId] = (accountCounts[accId] ?: 0) + 1
        }

        val totalWeight = accountWeights.values.sum()
        val topEntry = accountWeights.maxByOrNull { it.value }
        val topAcc = topEntry?.key?.let { accountById[it] }
        val topShare = if (totalWeight > 0.0 && topEntry != null) topEntry.value / totalWeight else 0.0
        val topCount = topEntry?.key?.let { accountCounts[it] } ?: 0
        val distinctHistoricalAccountsUsed = accountWeights.keys.size

        // Case A: Only 0 or 1 active account exists in the app -> no ambiguity possible
        if (activeAccounts.size <= 1) {
            val singleAcc = activeAccounts.firstOrNull()
            return LearnedTransactionPattern(
                inferredCategory = resolvedCategory,
                inferredTransactionType = resolvedType,
                inferredLocationName = resolvedLocation,
                preferredSourceAccount = if (resolvedType != TransactionType.INCOME) singleAcc else null,
                preferredDestinationAccount = if (resolvedType == TransactionType.INCOME) singleAcc else null,
                isAccountAutoSelectedFromHistory = topAcc != null,
                isCategoryAutoSelectedFromHistory = categoryLearnedFromHistory
            )
        }

        // Case B: Multiple active accounts exist in the app, check if Historical Pattern Learning can auto-select!
        // Auto-select if:
        // - Only 1 account was ever used for this merchant/category (at least 1 prior transaction), OR
        // - The top account has >= 68% weighted share across >= 2 prior transactions in this category/merchant.
        val hasStrongLearnedPattern = topAcc != null && (
            (distinctHistoricalAccountsUsed == 1 && topCount >= 1) ||
                (topShare >= 0.68 && topCount >= 2)
            )

        if (hasStrongLearnedPattern && topAcc != null) {
            return LearnedTransactionPattern(
                inferredCategory = resolvedCategory,
                inferredTransactionType = resolvedType,
                inferredLocationName = resolvedLocation,
                preferredSourceAccount = if (resolvedType != TransactionType.INCOME) topAcc else null,
                preferredDestinationAccount = if (resolvedType == TransactionType.INCOME) topAcc else null,
                isAccountAutoSelectedFromHistory = true,
                isCategoryAutoSelectedFromHistory = categoryLearnedFromHistory,
                requiresAccountDisambiguation = false,
                candidateAccounts = activeAccounts,
                patternExplanation = "Auto-selected ${topAcc.name} & $resolvedCategory based on your past transactions"
            )
        }

        // Case C: Multiple active accounts exist AND there is either no prior history for this category
        //         or the user has split transactions across multiple accounts -> Ask the user which account option to select!
        val orderedCandidates = activeAccounts.sortedByDescending { acc ->
            accountWeights[acc.id] ?: 0.0
        }

        return LearnedTransactionPattern(
            inferredCategory = resolvedCategory,
            inferredTransactionType = resolvedType,
            inferredLocationName = resolvedLocation,
            preferredSourceAccount = null,
            preferredDestinationAccount = null,
            isAccountAutoSelectedFromHistory = false,
            isCategoryAutoSelectedFromHistory = categoryLearnedFromHistory,
            requiresAccountDisambiguation = true,
            candidateAccounts = orderedCandidates,
            patternExplanation = "Multiple account options available"
        )
    }

    private fun matchExplicitAccount(
        hint: String,
        activeAccounts: List<AccountEntity>
    ): AccountEntity? {
        val clean = hint.trim()
        if (clean.isBlank()) return null
        return activeAccounts.firstOrNull { acc ->
            acc.name.equals(clean, ignoreCase = true) ||
                acc.name.contains(clean, ignoreCase = true) ||
                clean.contains(acc.name, ignoreCase = true) ||
                (acc.bankName.isNotBlank() && (acc.bankName.contains(clean, ignoreCase = true) || clean.contains(acc.bankName, ignoreCase = true)))
        }
    }

    private fun extractKeywords(text: String): Set<String> {
        return text
            .lowercase(Locale.US)
            .replace(Regex("""[^a-z\s]"""), " ")
            .split(Regex("""\s+"""))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in stopWords }
            .toSet()
    }
}
