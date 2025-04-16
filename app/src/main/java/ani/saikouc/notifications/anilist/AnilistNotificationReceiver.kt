package ani.saikouc.notifications.anilist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ani.saikouc.notifications.AlarmManagerScheduler
import ani.saikouc.notifications.TaskScheduler
import ani.saikouc.settings.saving.PrefManager
import ani.saikouc.settings.saving.PrefName
import ani.saikouc.util.Logger
import kotlinx.coroutines.runBlocking

class AnilistNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Logger.log("AnilistNotificationReceiver: onReceive")
        runBlocking {
            AnilistNotificationTask().execute(context)
        }
        val anilistInterval =
            AnilistNotificationWorker.checkIntervals[PrefManager.getVal(PrefName.AnilistNotificationInterval)]
        AlarmManagerScheduler(context).scheduleRepeatingTask(
            TaskScheduler.TaskType.ANILIST_NOTIFICATION,
            anilistInterval
        )
    }
}