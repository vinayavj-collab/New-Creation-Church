package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object AdminNotificationHelper {
    private const val CHANNEL_ID = "admin_security_otp_channel"
    private const val NOTIFICATION_ID_BASE = 8800

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "एडमिन सुरक्षा व OTP अलर्ट"
            val descriptionText = "नया P2 OTP और एडमिन सुरक्षा अलर्ट प्राप्त करें"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showOtpNotification(context: Context, adminName: String, otp: String, role: String? = null) {
        try {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val roleStr = if (!role.isNullOrBlank()) " ($role)" else ""
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("🔑 नया P2 OTP: $otp")
                .setContentText("एडमिन $adminName$roleStr के लिए 10-मिनट OTP तैयार है")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "एडमिन: $adminName$roleStr\n" +
                        "P2 OTP कोड: $otp\n" +
                        "वैधता: 10 मिनट के लिए मान्य\n" +
                        "सुरक्षा निर्देश: कृपया इसे केवल अधिकृत एडमिन के साथ ही साझा करें।"
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_BASE + (otp.hashCode() % 100), notif)
        } catch (_: SecurityException) {
            // Permission not granted or notification disabled
        } catch (_: Exception) {}
    }
}
