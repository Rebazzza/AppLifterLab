package com.example.lifterlab.ui.features.routines

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.lifterlab.data.RestTimerState

/**
 * Alarma del sistema para que el aviso de descanso no dependa de que Compose siga vivo.
 * La cuenta visual se mantiene en [RestTimerState], pero Android entrega este receiver
 * incluso si la actividad fue enviada al segundo plano.
 */
object RestTimerAlarm {
    private const val ACTION_FINISHED = "com.example.lifterlab.REST_TIMER_FINISHED"
    private const val EXTRA_EXERCISE = "exercise_name"
    private const val EXTRA_TARGET = "target_time_millis"
    private const val REQUEST_CODE = 4105
    private var lastFinishedTarget = 0L

    fun schedule(context: Context, exerciseName: String, targetTimeMillis: Long) {
        if (targetTimeMillis <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context, exerciseName, targetTimeMillis)
        alarm.cancel(pending)
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms() ->
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMillis, pending)
            else -> alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMillis, pending)
        }
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context, "", 0L)
        alarm.cancel(pending)
        pending.cancel()
    }

    @Synchronized
    fun finish(context: Context, exerciseName: String, targetTimeMillis: Long) {
        if (targetTimeMillis <= 0L || System.currentTimeMillis() < targetTimeMillis ||
            lastFinishedTarget == targetTimeMillis) return
        if (RestTimerState.isRunning && RestTimerState.targetTimeMillis != targetTimeMillis) return

        lastFinishedTarget = targetTimeMillis
        if (RestTimerState.targetTimeMillis == targetTimeMillis) RestTimerState.clear()
        cancel(context)
        RestTimerNotifier.notifyRestFinished(context, exerciseName)
    }

    private fun pendingIntent(context: Context, exerciseName: String, targetTimeMillis: Long): PendingIntent {
        val intent = Intent(context, RestTimerReceiver::class.java)
            .setAction(ACTION_FINISHED)
            .putExtra(EXTRA_EXERCISE, exerciseName)
            .putExtra(EXTRA_TARGET, targetTimeMillis)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun exerciseName(intent: Intent?): String = intent?.getStringExtra(EXTRA_EXERCISE).orEmpty()
    internal fun targetTimeMillis(intent: Intent?): Long = intent?.getLongExtra(EXTRA_TARGET, 0L) ?: 0L
}

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RestTimerAlarm.finish(context, RestTimerAlarm.exerciseName(intent), RestTimerAlarm.targetTimeMillis(intent))
    }
}
