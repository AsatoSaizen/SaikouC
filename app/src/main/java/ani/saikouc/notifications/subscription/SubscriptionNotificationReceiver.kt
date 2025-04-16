package ani.saikouc.notifications.subscription

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ani.saikouc.notifications.AlarmManagerScheduler
import ani.saikouc.notifications.TaskScheduler
import ani.saikouc.settings.saving.PrefManager
import ani.saikouc.settings.saving.PrefName
import ani.saikouc.util.Logger
import kotlinx.coroutines.runBlocking

class SubscriptionNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Logger.log("SubscriptionNotificationReceiver: onReceive")
        runBlocking {
            SubscriptionNotificationTask().execute(context)
        }
        val subscriptionInterval =
            SubscriptionNotificationWorker.checkIntervals[PrefManager.getVal(PrefName.SubscriptionNotificationInterval)]
        AlarmManagerScheduler(context).scheduleRepeatingTask(
            TaskScheduler.TaskType.SUBSCRIPTION_NOTIFICATION,
            subscriptionInterval
        )
    }
}