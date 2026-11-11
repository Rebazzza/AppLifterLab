package com.example.lifterlab.ui.features.routines

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Alarma del sistema para que el aviso de descanso no dependa de que Compose siga vivo.
 * La cuenta visual se mantiene en [RestTimerState], pero Android entrega este receiver
 * incluso si la actividad fue enviada al segundo plano.
 */
object RestTimerAlarm {
    private const val ACTION_FINISHED = "com.example.lifterlab.REST_TIMER_FINISHED"
    private const val EXTRA_EXERCISE = "exercise_name"
    private const val REQUEST_CODE = 4105

    fun schedule(context: Context, exerciseName: String, targetTimeMillis: Long) {
        if (targetTimeMillis <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context, exerciseName)
        alarm.cancel(pending)
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms() ->
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMillis, pending)
            else -> alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMillis, pending)
        }
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context, "")
        alarm.cancel(pending)
        pending.cancel()
    }

    private fun pendingIntent(context: Context, exerciseName: String): PendingIntent {
        val intent = Intent(context, RestTimerReceiver::class.java)
            .setAction(ACTION_FINISHED)
            .putExtra(EXTRA_EXERCISE, exerciseName)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun exerciseName(intent: Intent?): String = intent?.getStringExtra(EXTRA_EXERCISE).orEmpty()
}

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RestTimerNotifier.createNotificationChannel(context)
        RestTimerNotifier.notifyRestFinished(context, RestTimerAlarm.exerciseName(intent))
    }
}
