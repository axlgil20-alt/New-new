package com.example.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class PaymentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val commitmentId = intent.getLongExtra("commitment_id", 0L)
        val title = intent.getStringExtra("title") ?: "Compromiso de Pago"
        val beneficiary = intent.getStringExtra("beneficiary") ?: "Beneficiario"
        val amount = intent.getDoubleExtra("amount", 0.0)

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", 5) // Tab Pagos
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            commitmentId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = "Q ${"%,.2f".format(amount)}"
        val contentText = "Recuerda realizar tu pago de $formattedAmount a $beneficiary."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Recordatorio de Pago: $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText Mantén tus cuentas al día con Quetzi."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(commitmentId.toInt().coerceAtLeast(1001), notification)
        } catch (_: SecurityException) {
            // Permiso POST_NOTIFICATIONS no concedido; se ignora sin error
        }
    }

    companion object {
        const val CHANNEL_ID = "quetzi_payments_reminder"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Recordatorios de Pagos Quetzi"
                val descriptionText = "Notificaciones sobre fechas de vencimiento de compromisos familiares"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableLights(true)
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
