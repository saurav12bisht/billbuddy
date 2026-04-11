package com.android.billreminder.data.export

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.android.billreminder.data.repository.CustomerRepository
import com.android.billreminder.data.repository.TransactionRepository
import com.android.billreminder.domain.model.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ExportImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val customerRepository: CustomerRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormatSimple = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    data class ExportResult(
        val shareUri: Uri,
        val savedPath: String?
    )

    suspend fun exportToExcel(): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val transactions = transactionRepository.getAllTransactionsWithCustomerName()
            
            if (transactions.isEmpty()) {
                return@withContext Result.failure(Exception("No transactions to export"))
            }

            // Create temp file in app-specific storage for sharing
            val fileName = "Transaction_History_${System.currentTimeMillis()}.csv"
            val tempFile = File(context.cacheDir, fileName)

            val csvContent = StringBuilder()
            // Header
            // Header
            val headers = listOf(
                "Transaction ID", "Customer Name", "Customer Phone", "Customer ID", "Type", "Amount (â‚¹)",
                "Date", "Due Date", "Interest %", "Category", "Note", "Is Settlement", "Created At (Timeline)"
            )
            csvContent.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")

            // Data
            for (transaction in transactions) {
                val row = listOf(
                    transaction.id.toString(),
                    transaction.customerName,
                    transaction.customerPhone,
                    transaction.customerId.toString(),
                    transaction.type,
                    (transaction.amountPaise / 100.0).toString(),
                    formatDate(transaction.date),
                    transaction.dueDate?.let { formatDate(it) } ?: "",
                    transaction.interestPercent.toString(),
                    transaction.category,
                    transaction.note,
                    if (transaction.isSettlement) "Yes" else "No",
                    formatDateTime(transaction.createdAt)
                )
                csvContent.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
            }

            // Write to temp file
            FileOutputStream(tempFile).use { it.write(csvContent.toString().toByteArray()) }

            // Get Share URI
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )

            // Save to Downloads (User Request)
            val savedPath = saveToDownloads(fileName, csvContent.toString())

            Result.success(ExportResult(shareUri, savedPath))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveToDownloads(fileName: String, content: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                    "Downloads/$fileName"
                } else {
                    null
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                FileOutputStream(destFile).use { it.write(content.toByteArray()) }
                destFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importFromExcel(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Could not open file"))

            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // Read header and strip BOM if present
            var headerLine = reader.readLine()
            if (headerLine != null && headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1)
            }

            if (headerLine == null) {
                reader.close()
                return@withContext Result.failure(Exception("Empty file"))
            }

            val headers = parseCsvLine(headerLine)
            val columnMap = mutableMapOf<String, Int>()
            headers.forEachIndexed { index, header ->
                columnMap[header.trim()] = index
            }

            val transactions = mutableListOf<Transaction>()
            // Cache created customers to avoid inserting duplicates within the same import
            val createdCustomersCache = mutableMapOf<String, Int>() // Phone or Name -> ID
            
            var skippedCount = 0
            var errorCount = 0

            var line = reader.readLine()
            while (line != null) {
                try {
                    val rowData = parseCsvLine(line)
                    if (rowData.isNotEmpty()) {
                        
                        // Helper to safely get value
                        fun getValue(colName: String): String = 
                            columnMap[colName]?.let { if (it < rowData.size) rowData[it] else "" } ?: ""

                        // Transaction ID from CSV
                        val transactionIdStr = getValue("Transaction ID")
                        val transactionId = transactionIdStr.toLongOrNull() ?: 0L

                        val customerName = getValue("Customer Name")
                        val customerPhone = getValue("Customer Phone") // New field
                        val customerId = getValue("Customer ID").toIntOrNull() ?: 0
                        val type = getValue("Type")
                        val amount = getValue("Amount (â‚¹)").toDoubleOrNull() ?: 0.0
                        val dateStr = getValue("Date")
                        val dueDateStr = getValue("Due Date")
                        val interestPercent = getValue("Interest %").toDoubleOrNull() ?: 0.0
                        val category = getValue("Category")
                        val note = getValue("Note")
                        val isSettlement = getValue("Is Settlement").equals("Yes", ignoreCase = true)
                        val createdAtStr = getValue("Created At (Timeline)")

                        // Validate required fields
                        if (customerName.isEmpty() || type.isEmpty() || amount <= 0.0) {
                            skippedCount++
                        } else {
                            // CHECK FOR DUPLICATE TRANSACTION ID
                            val existingTransaction = if (transactionId > 0) {
                                transactionRepository.getById(transactionId)
                            } else {
                                null
                            }

                            if (existingTransaction != null) {
                                // Duplicate found, skip
                                skippedCount++
                            } else {
                                // Find customer by ID, Phone, or Name
                                // 1. Check local cache
                                var finalCustomerId = createdCustomersCache[customerPhone.ifEmpty { customerName.lowercase() }]
    
                                // 2. If not in cache, check repository 
                                if (finalCustomerId == null) {
                                    // PRIORITY 1: Match by Phone Number (if present in CSV)
                                    var existingCustomer = if (customerPhone.isNotEmpty()) {
                                        customerRepository.getCustomerByPhone(customerPhone)
                                    } else null

                                    // PRIORITY 2: Match by ID (legacy/internal) - REMOVED
                                    // Matching by ID is unsafe across devices as IDs are local auto-increments.
                                    // if (existingCustomer == null && customerId > 0) {
                                    //    existingCustomer = customerRepository.getCustomerById(customerId)
                                    // }

                                    // PRIORITY 3: Match by Name (Last resort, potentially risky but needed for old files)
                                    if (existingCustomer == null) {
                                        existingCustomer = customerRepository.getAllActiveCustomersList().firstOrNull { 
                                            it.name.equals(customerName, ignoreCase = true) 
                                        }
                                    }
                                    
                                    if (existingCustomer != null) {
                                        finalCustomerId = existingCustomer.id
                                    } else {
                                        // 3. Create new customer if not found
                                        val newCustomer = com.android.billreminder.domain.model.Customer(
                                            id = 0, // Auto-gen
                                            name = customerName.replace("\"", "").trim(),
                                            phone = customerPhone, // Use phone from CSV
                                            createdAt = System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        finalCustomerId = customerRepository.insert(newCustomer).toInt()
                                        createdCustomersCache[customerPhone.ifEmpty { customerName.lowercase() }] = finalCustomerId
                                    }
                                }
                                
                                val date = parseDate(dateStr) ?: System.currentTimeMillis()
                                val dueDate = if (dueDateStr.isNotEmpty()) parseDate(dueDateStr) else null
                                val createdAt = if (createdAtStr.isNotEmpty()) parseDateTime(createdAtStr) else System.currentTimeMillis()
                                val amountPaise = (amount * 100).toLong()
    
                                val transaction = Transaction(
                                    id = transactionId, // Use the ID from CSV
                                    customerId = finalCustomerId,
                                    type = type,
                                    amountPaise = amountPaise,
                                    date = date,
                                    dueDate = dueDate,
                                    interestPercent = interestPercent,
                                    category = category,
                                    note = note,
                                    receiptPhotoPath = null,
                                    isSettlement = isSettlement,
                                    createdAt = createdAt
                                )
    
                                transactions.add(transaction)
                            }
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    e.printStackTrace()
                }
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()

            if (transactions.isEmpty() && skippedCount == 0 && errorCount == 0) {
                 return@withContext Result.failure(Exception("No valid transactions found in file"))
            }

            // Insert transactions (logic remains same as before)
            if (transactions.isNotEmpty()) {
                transactionRepository.insertAll(transactions)
            }

            Result.success(
                ImportResult(
                    importedCount = transactions.size,
                    skippedCount = skippedCount,
                    errorCount = errorCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    current.append(c)
                }
            } else {
                if (c == '"') {
                    inQuotes = true
                } else if (c == ',') {
                    result.add(current.toString())
                    current = StringBuilder()
                } else {
                    current.append(c)
                }
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun parseDate(dateStr: String): Long? {
        return try {
            dateFormatSimple.parse(dateStr)?.time
        } catch (e: Exception) {
            try {
                dateFormat.parse(dateStr)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun parseDateTime(dateTimeStr: String): Long {
        return try {
            dateFormat.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun formatDate(timestamp: Long): String {
        return dateFormatSimple.format(Date(timestamp))
    }

    private fun formatDateTime(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    data class ImportResult(
        val importedCount: Int,
        val skippedCount: Int,
        val errorCount: Int
    )
}
