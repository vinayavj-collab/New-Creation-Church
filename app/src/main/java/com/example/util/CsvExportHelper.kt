package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        var str = value.trim()
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }

    private fun getExportFile(context: Context, prefix: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(exportDir, "${prefix}_$dateStamp.csv")
    }

    private fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun exportMembersToCsv(context: Context, members: List<ChurchMember>): Uri? {
        return try {
            val file = getExportFile(context, "church_members")
            FileWriter(file).use { writer ->
                // Header
                writer.append("ID,Name,Phone,Email,Address,Family Role,Family Name,Baptism Status,Birth Date,Anniversary Date,Status,Notes,Added By,Created Timestamp\n")
                // Rows
                for (m in members) {
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(m.timestamp))
                    writer.append(escapeCsv(m.id)).append(",")
                    writer.append(escapeCsv(m.name)).append(",")
                    writer.append(escapeCsv(m.phone)).append(",")
                    writer.append(escapeCsv(m.email)).append(",")
                    writer.append(escapeCsv(m.address)).append(",")
                    writer.append(escapeCsv(m.familyRole)).append(",")
                    writer.append(escapeCsv(m.familyName)).append(",")
                    writer.append(escapeCsv(m.baptismStatus)).append(",")
                    writer.append(escapeCsv(m.birthDate)).append(",")
                    writer.append(escapeCsv(m.anniversaryDate)).append(",")
                    writer.append(escapeCsv(m.status)).append(",")
                    writer.append(escapeCsv(m.notes)).append(",")
                    writer.append(escapeCsv(m.addedByAdmin)).append(",")
                    writer.append(escapeCsv(dateFormatted)).append("\n")
                }
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportAttendanceToCsv(context: Context, attendance: List<ChurchAttendanceRecord>): Uri? {
        return try {
            val file = getExportFile(context, "church_attendance")
            FileWriter(file).use { writer ->
                // Header
                writer.append("ID,Date,Service Type,Men,Women,Children,Total Count,New Visitors,Topic or Preacher,Notes,Recorded By,Timestamp\n")
                // Rows
                for (a in attendance) {
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(a.timestamp))
                    writer.append(escapeCsv(a.id)).append(",")
                    writer.append(escapeCsv(a.dateString)).append(",")
                    writer.append(escapeCsv(a.serviceType)).append(",")
                    writer.append(a.countMen.toString()).append(",")
                    writer.append(a.countWomen.toString()).append(",")
                    writer.append(a.countChildren.toString()).append(",")
                    writer.append(a.totalCount.toString()).append(",")
                    writer.append(a.newVisitorsCount.toString()).append(",")
                    writer.append(escapeCsv(a.topicOrPreacher)).append(",")
                    writer.append(escapeCsv(a.notes)).append(",")
                    writer.append(escapeCsv(a.recordedByAdmin)).append(",")
                    writer.append(escapeCsv(dateFormatted)).append("\n")
                }
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportAccountsToCsv(context: Context, transactions: List<ChurchAccountTransaction>): Uri? {
        return try {
            val file = getExportFile(context, "church_accounts")
            FileWriter(file).use { writer ->
                // Header
                writer.append("ID,Date,Type,Category,Amount (INR),Donor or Recipient,Payment Mode,Notes,Recorded By,Timestamp\n")
                // Rows
                for (t in transactions) {
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(t.timestamp))
                    writer.append(escapeCsv(t.id)).append(",")
                    writer.append(escapeCsv(t.dateString)).append(",")
                    writer.append(escapeCsv(t.type)).append(",")
                    writer.append(escapeCsv(t.category)).append(",")
                    writer.append(t.amount.toString()).append(",")
                    writer.append(escapeCsv(t.donorOrRecipient)).append(",")
                    writer.append(escapeCsv(t.paymentMode)).append(",")
                    writer.append(escapeCsv(t.notes)).append(",")
                    writer.append(escapeCsv(t.recordedByAdmin)).append(",")
                    writer.append(escapeCsv(dateFormatted)).append("\n")
                }
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportAdminsToCsv(context: Context, admins: List<AdminUser>): Uri? {
        return try {
            val file = getExportFile(context, "admin_users")
            FileWriter(file).use { writer ->
                // Header
                writer.append("ID,Name,Designation,Rank,Linked Gmail,Status,Assigned Functions,Permissions,Last Login Timestamp\n")
                // Rows
                for (admin in admins) {
                    val dateFormatted = if (admin.lastLoginTimestamp > 0) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(admin.lastLoginTimestamp))
                    } else "Never"
                    writer.append(escapeCsv(admin.id)).append(",")
                    writer.append(escapeCsv(admin.name)).append(",")
                    writer.append(escapeCsv(admin.designation)).append(",")
                    writer.append(admin.rank.toString()).append(",")
                    writer.append(escapeCsv(admin.linkedGmail)).append(",")
                    writer.append(if (admin.isEnabled) "Active" else "Disabled").append(",")
                    writer.append(escapeCsv(admin.assignedFunctions.joinToString("; "))).append(",")
                    writer.append(escapeCsv(admin.permissions.joinToString("; "))).append(",")
                    writer.append(escapeCsv(dateFormatted)).append("\n")
                }
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportAuditLogsToCsv(context: Context, auditLogs: List<AdminAuditLog>): Uri? {
        return try {
            val file = getExportFile(context, "admin_audit_logs")
            FileWriter(file).use { writer ->
                // Header
                writer.append("Log ID,Admin ID,Admin Name,Designation,Action Type,Description,Target ID,Timestamp\n")
                // Rows
                for (log in auditLogs) {
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    writer.append(escapeCsv(log.id)).append(",")
                    writer.append(escapeCsv(log.adminId)).append(",")
                    writer.append(escapeCsv(log.adminName)).append(",")
                    writer.append(escapeCsv(log.adminDesignation)).append(",")
                    writer.append(escapeCsv(log.actionType)).append(",")
                    writer.append(escapeCsv(log.description)).append(",")
                    writer.append(escapeCsv(log.targetId)).append(",")
                    writer.append(escapeCsv(dateFormatted)).append("\n")
                }
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportQrAuditLogsToCsv(context: Context, qrLogs: List<QrAuditLogEntry>): Uri? {
        return try {
            val file = getExportFile(context, "qr_otp_audit_trail")
            FileWriter(file).use { writer ->
                // Header
                writer.append("Log ID,Target Serial ID,Target Name,Target Role/Designation,Generated By Admin,Admin Designation,Issuance Timestamp,OTP Code,OTP Expiry,OTP Activation Status,QR Payload Type,Activated Timestamp,Activated Device,Notes\n")
                // Rows
                for (log in qrLogs) {
                    val issuedFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    val expiryFormatted = if (log.otpExpiresAt > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.otpExpiresAt)) else "N/A"
                    val activatedFormatted = if (log.activatedTimestamp > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.activatedTimestamp)) else "Not Activated"

                    writer.append(escapeCsv(log.id)).append(",")
                    writer.append(escapeCsv(log.targetSerial)).append(",")
                    writer.append(escapeCsv(log.targetName)).append(",")
                    writer.append(escapeCsv("${log.targetDesignation} (${log.targetRoleTier})")).append(",")
                    writer.append(escapeCsv(log.generatedByAdminName)).append(",")
                    writer.append(escapeCsv(log.generatedByAdminDesignation)).append(",")
                    writer.append(escapeCsv(issuedFormatted)).append(",")
                    writer.append(escapeCsv(log.otpCode.ifBlank { "******" })).append(",")
                    writer.append(escapeCsv(expiryFormatted)).append(",")
                    writer.append(escapeCsv(log.otpStatus)).append(",")
                    writer.append(escapeCsv(log.qrPayloadType)).append(",")
                    writer.append(escapeCsv(activatedFormatted)).append(",")
                    writer.append(escapeCsv(log.activatedDeviceId.ifBlank { "N/A" })).append(",")
                    writer.append(escapeCsv(log.notes)).append("\n")
                }
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportPushNotificationsToCsv(context: Context, pushList: List<AdminPushNotification>): Uri? {
        return try {
            val file = getExportFile(context, "push_notifications")
            FileWriter(file).use { writer ->
                // Header
                writer.append("ID,Title,Body,Target Audience,Urgency,Action URL,Sent By Admin,Timestamp\n")
                // Rows
                for (p in pushList) {
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(p.timestamp))
                    writer.append(escapeCsv(p.id)).append(",")
                    writer.append(escapeCsv(p.title)).append(",")
                    writer.append(escapeCsv(p.body)).append(",")
                    writer.append(escapeCsv(p.targetAudience)).append(",")
                    writer.append(escapeCsv(p.urgency)).append(",")
                    writer.append(escapeCsv(p.actionUrl)).append(",")
                    writer.append(escapeCsv(p.sentByAdmin)).append(",")
                    writer.append(escapeCsv(dateFormatted)).append("\n")
                }
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvFile(context: Context, uri: Uri, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title - कलीसिया मास्टर एडमिन ऑफ़लाइन रिपोर्ट (CSV File)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "CSV रिपोर्ट साझा करें (Export CSV)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareMultipleCsvFiles(context: Context, uris: ArrayList<Uri>, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title - कलीसिया समस्त डेटाबेस ऑफ़लाइन रिपोर्ट (All CSV Reports)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "समस्त CSV रिपोर्ट साझा करें (Export All CSV)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
