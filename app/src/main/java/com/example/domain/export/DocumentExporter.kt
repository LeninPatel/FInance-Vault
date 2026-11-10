package com.example.domain.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.domain.money.InrMoney
import com.example.domain.period.IndianFinancialYear
import com.example.domain.reports.FinancialReportGenerator
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates and downloads professional financial documents in PDF (.pdf),
 * native Excel OpenXML (.xlsx), CSV (.csv), and JSON Backup (.json) formats.
 * Automatically saves downloadable copies directly to the device's Downloads folder.
 */
object DocumentExporter {

    data class ExportedDocumentResult(
        val file: File,
        val mimeType: String,
        val previewText: String,
        val title: String,
        val downloadedUri: Uri? = null,
        val downloadedLocationLabel: String = ""
    )

    /**
     * Builds a comprehensive text preview of all Indian accounting reports for the selected period.
     */
    fun buildFullReportPreviewText(
        userName: String,
        financialYear: String,
        periodLabel: String,
        generationDate: String,
        balanceSheet: FinancialReportGenerator.BalanceSheetReport,
        profitAndLoss: FinancialReportGenerator.ProfitAndLossReport,
        cashFlow: FinancialReportGenerator.CashFlowReport,
        trialBalance: FinancialReportGenerator.TrialBalanceReport,
        journalBook: FinancialReportGenerator.JournalBookReport
    ): String {
        val holder = userName.ifBlank { "Not provided" }
        return buildString {
            appendLine("====================================================")
            appendLine("FINANCE VAULT — INDIAN FINANCIAL STATEMENTS (INR ₹)")
            appendLine("Prepared For: $holder")
            appendLine("Financial Year: $financialYear | Period: $periodLabel")
            appendLine("Generated On: $generationDate")
            appendLine("====================================================")
            appendLine()
            appendLine("1. BALANCE SHEET AS AT ${IndianFinancialYear.formatEpochDay(balanceSheet.asAtEpochDay)}")
            appendLine("----------------------------------------------------")
            appendLine("ASSETS")
            appendLine("  Non-Current Assets:")
            appendLine("    Investments                         : ${InrMoney.formatPaise(balanceSheet.nonCurrentInvestmentsPaise, showZeroPaise = true)}")
            appendLine("    Property & Long-Term Assets         : ${InrMoney.formatPaise(balanceSheet.nonCurrentPropertyAndOtherPaise, showZeroPaise = true)}")
            appendLine("  Current Assets:")
            appendLine("    Cash and Cash Equivalents           : ${InrMoney.formatPaise(balanceSheet.currentCashAndEquivalentsPaise, showZeroPaise = true)}")
            appendLine("    Bank Balances                       : ${InrMoney.formatPaise(balanceSheet.currentBankBalancesPaise, showZeroPaise = true)}")
            appendLine("    Other Current Assets                : ${InrMoney.formatPaise(balanceSheet.currentOtherAssetsPaise, showZeroPaise = true)}")
            appendLine("  TOTAL ASSETS                          : ${InrMoney.formatPaise(balanceSheet.totalAssetsPaise, showZeroPaise = true)}")
            appendLine()
            appendLine("LIABILITIES")
            appendLine("  Non-Current Liabilities:")
            appendLine("    Loans and Borrowings                : ${InrMoney.formatPaise(balanceSheet.nonCurrentLoansAndBorrowingsPaise, showZeroPaise = true)}")
            appendLine("  Current Liabilities:")
            appendLine("    Credit Card Outstanding             : ${InrMoney.formatPaise(balanceSheet.currentCreditCardOutstandingPaise, showZeroPaise = true)}")
            appendLine("    Other Payables                      : ${InrMoney.formatPaise(balanceSheet.currentOtherPayablesPaise, showZeroPaise = true)}")
            appendLine("  TOTAL LIABILITIES                     : ${InrMoney.formatPaise(balanceSheet.totalLiabilitiesPaise, showZeroPaise = true)}")
            appendLine()
            appendLine("PERSONAL NET WORTH")
            appendLine("    Opening Capital                     : ${InrMoney.formatPaise(balanceSheet.openingCapitalPaise, showZeroPaise = true)}")
            appendLine("    Additional Contributions            : ${InrMoney.formatPaise(balanceSheet.additionalContributionsPaise, showZeroPaise = true)}")
            appendLine("    Accumulated Surplus / (Deficit)     : ${InrMoney.formatPaise(balanceSheet.accumulatedSurplusPaise, showZeroPaise = true)}")
            appendLine("    Less: Drawings                      : ${InrMoney.formatPaise(balanceSheet.drawingsPaise, showZeroPaise = true)}")
            appendLine("  TOTAL NET WORTH                       : ${InrMoney.formatPaise(balanceSheet.totalNetWorthPaise, showZeroPaise = true)}")
            appendLine("  TOTAL LIABILITIES AND NET WORTH       : ${InrMoney.formatPaise(balanceSheet.totalLiabilitiesAndNetWorthPaise, showZeroPaise = true)}")
            appendLine("  Reconciliation Status                 : ${if (balanceSheet.isReconciled) "VERIFIED (Assets = Liabilities + Net Worth)" else "UNBALANCED"}")
            appendLine()
            appendLine("2. STATEMENT OF PROFIT AND LOSS")
            appendLine("----------------------------------------------------")
            appendLine("  Salary Income                         : ${InrMoney.formatPaise(profitAndLoss.salaryIncomePaise, showZeroPaise = true)}")
            appendLine("  Business Income                       : ${InrMoney.formatPaise(profitAndLoss.businessIncomePaise, showZeroPaise = true)}")
            appendLine("  Interest Income                       : ${InrMoney.formatPaise(profitAndLoss.interestIncomePaise, showZeroPaise = true)}")
            appendLine("  Dividend Income                       : ${InrMoney.formatPaise(profitAndLoss.dividendIncomePaise, showZeroPaise = true)}")
            appendLine("  Realized Capital Gains                : ${InrMoney.formatPaise(profitAndLoss.capitalGainIncomePaise, showZeroPaise = true)}")
            appendLine("  Other Income                          : ${InrMoney.formatPaise(profitAndLoss.otherIncomePaise, showZeroPaise = true)}")
            appendLine("  TOTAL INCOME                          : ${InrMoney.formatPaise(profitAndLoss.totalIncomePaise, showZeroPaise = true)}")
            appendLine("  Household Expenses                    : ${InrMoney.formatPaise(profitAndLoss.householdExpensesPaise, showZeroPaise = true)}")
            appendLine("  Education Expenses                    : ${InrMoney.formatPaise(profitAndLoss.educationExpensesPaise, showZeroPaise = true)}")
            appendLine("  Transportation Expenses               : ${InrMoney.formatPaise(profitAndLoss.transportationExpensesPaise, showZeroPaise = true)}")
            appendLine("  Utilities                             : ${InrMoney.formatPaise(profitAndLoss.utilitiesExpensesPaise, showZeroPaise = true)}")
            appendLine("  Healthcare                            : ${InrMoney.formatPaise(profitAndLoss.healthcareExpensesPaise, showZeroPaise = true)}")
            appendLine("  Interest Expenses                     : ${InrMoney.formatPaise(profitAndLoss.interestExpensesPaise, showZeroPaise = true)}")
            appendLine("  Bank Charges                          : ${InrMoney.formatPaise(profitAndLoss.bankChargesPaise, showZeroPaise = true)}")
            appendLine("  Taxes & Other Expenses                : ${InrMoney.formatPaise(profitAndLoss.taxesAndOtherExpensesPaise + profitAndLoss.investmentFeesPaise, showZeroPaise = true)}")
            appendLine("  TOTAL EXPENSES                        : ${InrMoney.formatPaise(profitAndLoss.totalExpensesPaise, showZeroPaise = true)}")
            appendLine("  SURPLUS / DEFICIT FOR THE PERIOD      : ${InrMoney.formatPaise(profitAndLoss.surplusOrDeficitPaise, showZeroPaise = true)}")
            appendLine()
            appendLine("3. CASH FLOW STATEMENT (Adapted Personal Format)")
            appendLine("----------------------------------------------------")
            appendLine("  Net Cash from Operating Activities    : ${InrMoney.formatPaise(cashFlow.netCashFromOperatingPaise, showZeroPaise = true)}")
            appendLine("  Net Cash from Investing Activities    : ${InrMoney.formatPaise(cashFlow.netCashFromInvestingPaise, showZeroPaise = true)}")
            appendLine("  Net Cash from Financing Activities    : ${InrMoney.formatPaise(cashFlow.netCashFromFinancingPaise, showZeroPaise = true)}")
            appendLine("  Net Change in Cash & Bank             : ${InrMoney.formatPaise(cashFlow.netChangeInCashAndBankPaise, showZeroPaise = true)}")
            appendLine("  Opening Cash & Bank Equivalents       : ${InrMoney.formatPaise(cashFlow.openingCashAndBankPaise, showZeroPaise = true)}")
            appendLine("  Closing Cash & Bank Equivalents       : ${InrMoney.formatPaise(cashFlow.closingCashAndBankPaise, showZeroPaise = true)}")
            appendLine()
            appendLine("4. TRIAL BALANCE")
            appendLine("----------------------------------------------------")
            for (row in trialBalance.rows) {
                val dr = if (row.debitBalancePaise > 0) InrMoney.formatPaise(row.debitBalancePaise) else "—"
                val cr = if (row.creditBalancePaise > 0) InrMoney.formatPaise(row.creditBalancePaise) else "—"
                appendLine("  ${row.accountName.padEnd(30)} | Dr: ${dr.padStart(12)} | Cr: ${cr.padStart(12)}")
            }
            appendLine("  TOTAL DEBITS                          : ${InrMoney.formatPaise(trialBalance.totalDebitPaise, showZeroPaise = true)}")
            appendLine("  TOTAL CREDITS                         : ${InrMoney.formatPaise(trialBalance.totalCreditPaise, showZeroPaise = true)}")
            appendLine()
            appendLine("5. JOURNAL BOOK (${journalBook.entriesCount} Posted Vouchers)")
            appendLine("  Total Debits: ${InrMoney.formatPaise(journalBook.totalDebitPaise)} | Total Credits: ${InrMoney.formatPaise(journalBook.totalCreditPaise)}")
            appendLine("====================================================")
            appendLine("Finance Vault • Made by Lenin and Shaswat")
        }
    }

    /**
     * Generates a multi-page PDF document using Android's PdfDocument API
     * and saves a downloadable copy to the device's Downloads folder.
     */
    fun exportFinancialReportPdf(
        context: Context,
        title: String,
        reportText: String
    ): ExportedDocumentResult {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "FinanceVault_Statement_${System.currentTimeMillis()}.pdf"
        val file = File(exportDir, fileName)

        writeStandardPdf14File(file, title, reportText)

        val mimeType = "application/pdf"
        val (dlUri, dlLabel) = saveFileToDownloads(context, file, mimeType)

        return ExportedDocumentResult(
            file = file,
            mimeType = mimeType,
            previewText = reportText,
            title = "$title (PDF)",
            downloadedUri = dlUri,
            downloadedLocationLabel = dlLabel
        )
    }

    /**
     * Pure-Kotlin standard PDF 1.4 writer generating multi-page A4 financial statements
     * with styled header banner, bold section headings, monospaced tables, and page footers.
     */
    private fun writeStandardPdf14File(targetFile: File, title: String, reportText: String) {
        fun escapePdfString(input: String): String {
            return input
                .replace("₹", "Rs.")
                .replace("•", "-")
                .replace("—", "-")
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .map { ch -> if (ch.code in 32..126) ch else ' ' }
                .joinToString("")
        }

        val lines = reportText.lines()
        val linesPerPage = 48
        val totalPages = ((lines.size + linesPerPage - 1) / linesPerPage).coerceAtLeast(1)

        // Object IDs:
        // 1: Catalog, 2: Pages, 3: Font Courier, 4: Font Helvetica-Bold, 5: Font Courier-Bold
        // For each page i (0 until totalPages):
        //   pageObjId = 6 + i * 2
        //   contentObjId = 7 + i * 2
        val objects = mutableMapOf<Int, String>()
        val pageKids = (0 until totalPages).joinToString(" ") { "${6 + it * 2} 0 R" }

        objects[1] = "<< /Type /Catalog /Pages 2 0 R >>"
        objects[2] = "<< /Type /Pages /Kids [ $pageKids ] /Count $totalPages >>"
        objects[3] = "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>"
        objects[4] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"
        objects[5] = "<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold >>"

        for (pageIndex in 0 until totalPages) {
            val pageObjId = 6 + pageIndex * 2
            val contentObjId = 7 + pageIndex * 2
            val startLine = pageIndex * linesPerPage
            val endLine = (startLine + linesPerPage).coerceAtMost(lines.size)

            val streamContent = buildString {
                if (pageIndex == 0) {
                    // Draw dark obsidian header box and emerald left accent bar
                    appendLine("0.055 0.086 0.149 rg")
                    appendLine("32 764 531 50 re f")
                    appendLine("0.063 0.725 0.506 rg")
                    appendLine("32 764 5 50 re f")
                }
                appendLine("BT")
                if (pageIndex == 0) {
                    appendLine("0.063 0.725 0.506 rg")
                    appendLine("/F2 12 Tf")
                    appendLine("44 794 Td (${escapePdfString(title)}) Tj")
                    appendLine("0.92 0.95 0.98 rg")
                    appendLine("/F1 9 Tf")
                    appendLine("0 -16 Td (${escapePdfString("Finance Vault - Made by Lenin and Shaswat - Indian Rupee (INR Rs.)")}) Tj")
                    appendLine("-4 0 Td")
                    appendLine("0 -28 Td () Tj")
                } else {
                    appendLine("40 795 Td () Tj")
                }
                for (i in startLine until endLine) {
                    val rawLine = lines[i]
                    val safeLine = escapePdfString(rawLine.take(92))
                    val isSectionHeader = rawLine.startsWith("1.") || rawLine.startsWith("2.") ||
                        rawLine.startsWith("3.") || rawLine.startsWith("4.") ||
                        rawLine.startsWith("5.") || rawLine.contains("TOTAL ")
                    if (isSectionHeader) {
                        appendLine("0.02 0.52 0.36 rg")
                        appendLine("/F3 9.5 Tf")
                    } else {
                        appendLine("0.08 0.12 0.18 rg")
                        appendLine("/F1 9 Tf")
                    }
                    appendLine("0 -14 Td ($safeLine) Tj")
                }
                appendLine("0.39 0.45 0.55 rg")
                appendLine("/F1 8.5 Tf")
                appendLine("0 -22 Td (${escapePdfString("Finance Vault - Made by Lenin and Shaswat - INR (Rs.) - Page ${pageIndex + 1} of $totalPages")}) Tj")
                appendLine("ET")
            }

            val streamBytes = streamContent.toByteArray(Charsets.US_ASCII)
            objects[pageObjId] = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> /Contents $contentObjId 0 R >>"
            objects[contentObjId] = "<< /Length ${streamBytes.size} >>\nstream\n$streamContent\nendstream"
        }

        val maxObjId = 5 + totalPages * 2
        val out = java.io.ByteArrayOutputStream()
        fun writeAscii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))

        writeAscii("%PDF-1.4\n")
        val offsets = IntArray(maxObjId + 1)
        for (id in 1..maxObjId) {
            offsets[id] = out.size()
            writeAscii("$id 0 obj\n${objects[id]}\nendobj\n")
        }
        val xrefOffset = out.size()
        writeAscii("xref\n0 ${maxObjId + 1}\n")
        writeAscii("0000000000 65535 f \n")
        for (id in 1..maxObjId) {
            writeAscii(String.format(java.util.Locale.US, "%010d 00000 n \n", offsets[id]))
        }
        writeAscii("trailer\n<< /Size ${maxObjId + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
        targetFile.writeBytes(out.toByteArray())
    }

    /**
     * Generates either a genuine OpenXML Excel workbook (.xlsx) or a standard CSV (.csv)
     * and automatically saves a downloadable copy to the device's Downloads folder.
     */
    fun exportFinancialReportCsv(
        context: Context,
        financialYear: String,
        balanceSheet: FinancialReportGenerator.BalanceSheetReport,
        profitAndLoss: FinancialReportGenerator.ProfitAndLossReport,
        trialBalance: FinancialReportGenerator.TrialBalanceReport,
        journalBook: FinancialReportGenerator.JournalBookReport,
        asExcelExtension: Boolean = false
    ): ExportedDocumentResult {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("Section", "Date / Code", "Particulars / Account Name", "Ledger Folio", "Debit (INR)", "Credit (INR)"))
        rows.add(listOf("Summary", financialYear, "Total Assets", "", InrMoney.formatPaise(balanceSheet.totalAssetsPaise, includeSymbol = false), "0.00"))
        rows.add(listOf("Summary", financialYear, "Total Liabilities", "", "0.00", InrMoney.formatPaise(balanceSheet.totalLiabilitiesPaise, includeSymbol = false)))
        rows.add(listOf("Summary", financialYear, "Personal Net Worth", "", "0.00", InrMoney.formatPaise(balanceSheet.totalNetWorthPaise, includeSymbol = false)))
        rows.add(listOf("Summary", financialYear, "Total Income", "", "0.00", InrMoney.formatPaise(profitAndLoss.totalIncomePaise, includeSymbol = false)))
        rows.add(listOf("Summary", financialYear, "Total Expenses", "", InrMoney.formatPaise(profitAndLoss.totalExpensesPaise, includeSymbol = false), "0.00"))
        rows.add(listOf("Summary", financialYear, "Surplus / Deficit", "", "0.00", InrMoney.formatPaise(profitAndLoss.surplusOrDeficitPaise, includeSymbol = false)))

        for (row in trialBalance.rows) {
            rows.add(
                listOf(
                    "Trial Balance",
                    row.accountCode,
                    row.accountName,
                    row.ledgerFolio,
                    InrMoney.paiseToPlainRupeeInput(row.debitBalancePaise),
                    InrMoney.paiseToPlainRupeeInput(row.creditBalancePaise)
                )
            )
        }

        for (jRow in journalBook.rows.filter { !it.isNarrationRow }) {
            rows.add(
                listOf(
                    "Journal Book",
                    IndianFinancialYear.formatShortEpochDay(jRow.dateEpochDay),
                    "${jRow.particulars.trim()} (${jRow.voucherNumber})",
                    jRow.ledgerFolio,
                    InrMoney.paiseToPlainRupeeInput(jRow.debitPaise),
                    InrMoney.paiseToPlainRupeeInput(jRow.creditPaise)
                )
            )
        }

        val csvPreview = buildString {
            for (r in rows) {
                appendLine(r.joinToString(",") { cell -> cell.replace(",", " ") })
            }
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = System.currentTimeMillis()

        return if (asExcelExtension) {
            val file = File(exportDir, "FinanceVault_Statements_$timestamp.xlsx")
            writeOpenXmlXlsxFile(file, rows)
            val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            val (dlUri, dlLabel) = saveFileToDownloads(context, file, mimeType)
            ExportedDocumentResult(
                file = file,
                mimeType = mimeType,
                previewText = csvPreview,
                title = "Excel Workbook (.xlsx)",
                downloadedUri = dlUri,
                downloadedLocationLabel = dlLabel
            )
        } else {
            val file = File(exportDir, "FinanceVault_Statements_$timestamp.csv")
            file.writeText(csvPreview, Charsets.UTF_8)
            val mimeType = "text/csv"
            val (dlUri, dlLabel) = saveFileToDownloads(context, file, mimeType)
            ExportedDocumentResult(
                file = file,
                mimeType = mimeType,
                previewText = csvPreview,
                title = "CSV Spreadsheet (.csv)",
                downloadedUri = dlUri,
                downloadedLocationLabel = dlLabel
            )
        }
    }

    /**
     * Writes a genuine OpenXML (.xlsx) spreadsheet file so Excel and Google Sheets open it natively.
     */
    private fun writeOpenXmlXlsxFile(targetFile: File, rows: List<List<String>>) {
        fun escapeXml(s: String): String = s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Financial Statements" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

        val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            rows.forEachIndexed { rowIdx, rowCells ->
                val rNum = rowIdx + 1
                append("""<row r="$rNum">""")
                rowCells.forEachIndexed { colIdx, cellValue ->
                    val colLetter = ('A' + colIdx)
                    val cellRef = "$colLetter$rNum"
                    val numericVal = if (rowIdx > 0 && colIdx >= 4) cellValue.replace(",", "").toDoubleOrNull() else null
                    if (numericVal != null) {
                        append("""<c r="$cellRef"><v>$numericVal</v></c>""")
                    } else {
                        append("""<c r="$cellRef" t="inlineStr"><is><t>${escapeXml(cellValue)}</t></is></c>""")
                    }
                }
                append("""</row>""")
            }
            append("""</sheetData></worksheet>""")
        }

        ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
            fun addEntry(path: String, content: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            addEntry("[Content_Types].xml", contentTypesXml)
            addEntry("_rels/.rels", relsXml)
            addEntry("xl/workbook.xml", workbookXml)
            addEntry("xl/_rels/workbook.xml.rels", workbookRelsXml)
            addEntry("xl/worksheets/sheet1.xml", sheetXml)
        }
    }

    /**
     * Writes JSON backup file and automatically saves a downloadable copy to the device's Downloads folder.
     */
    fun exportJsonBackupFile(
        context: Context,
        jsonContent: String
    ): ExportedDocumentResult {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "FinanceVault_Backup_${System.currentTimeMillis()}.json")
        file.writeText(jsonContent, Charsets.UTF_8)
        val mimeType = "application/json"
        val (dlUri, dlLabel) = saveFileToDownloads(context, file, mimeType)
        return ExportedDocumentResult(
            file = file,
            mimeType = mimeType,
            previewText = jsonContent,
            title = "JSON Vault Backup (.json)",
            downloadedUri = dlUri,
            downloadedLocationLabel = dlLabel
        )
    }

    /**
     * Saves the exported file directly into the device's public Downloads folder (zero permissions needed on API 29+).
     */
    fun saveFileToDownloads(context: Context, sourceFile: File, mimeType: String): Pair<Uri?, String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FinanceVault")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    return uri to "Downloads/FinanceVault/${sourceFile.name}"
                }
            }
            // Fallback to app external downloads directory
            val fallbackDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "FinanceVault").apply { mkdirs() }
            val destFile = File(fallbackDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)
            val authority = "${context.packageName}.fileprovider"
            val fileUri = runCatching { FileProvider.getUriForFile(context, authority, destFile) }.getOrNull()
            fileUri to "Downloads/${sourceFile.name}"
        } catch (_: Exception) {
            null to sourceFile.name
        }
    }

    /**
     * Copies the exported file to a user-chosen destination URI from ActivityResultContracts.CreateDocument.
     */
    fun copyFileToUri(context: Context, sourceFile: File, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens the exported PDF/Excel/CSV/JSON file directly in a viewer app, or falls back to Share Sheet.
     */
    fun openExportedFile(context: Context, result: ExportedDocumentResult) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, result.file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, result.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open ${result.title}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Throwable) {
            shareExportedFile(context, result)
        }
    }

    /**
     * Shares a generated file via Android Share Sheet using the registered FileProvider.
     */
    fun shareExportedFile(context: Context, result: ExportedDocumentResult) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, result.file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = result.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, result.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Download / Share ${result.title}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Throwable) {
            runCatching {
                val textIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, result.title)
                    putExtra(Intent.EXTRA_TEXT, result.previewText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(textIntent, "Share ${result.title}").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }
}
