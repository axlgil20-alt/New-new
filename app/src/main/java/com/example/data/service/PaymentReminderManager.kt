package com.example.data.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.PaymentCommitmentEntity

object PaymentReminderManager {

    fun scheduleReminder(context: Context, commitment: PaymentCommitmentEntity) {
        if (commitment.reminderDaysBefore < 0 || commitment.isPaid) {
            cancelReminder(context, commitment.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderOffsetMillis = commitment.reminderDaysBefore * 24L * 60L * 60L * 1000L
        val triggerTime = commitment.dueDateMillis - reminderOffsetMillis

        // Solo programar si el vencimiento/recordatorio está en el futuro
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
            putExtra("commitment_id", commitment.id)
            putExtra("title", commitment.title)
            putExtra("beneficiary", commitment.beneficiaryName)
            putExtra("amount", commitment.amount)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            commitment.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Manejo de restricciones en Android 12+ para alarmas exactas
        }
    }

    fun cancelReminder(context: Context, commitmentId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PaymentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            commitmentId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
