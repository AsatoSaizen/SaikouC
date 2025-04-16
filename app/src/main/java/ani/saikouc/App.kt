package ani.saikouc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import ani.saikouc.addons.download.DownloadAddonManager
import ani.saikouc.addons.torrent.TorrentAddonManager
import ani.saikouc.aniyomi.anime.custom.AppModule
import ani.saikouc.aniyomi.anime.custom.PreferenceModule
import ani.saikouc.connections.comments.CommentsAPI
import ani.saikouc.connections.crashlytics.CrashlyticsInterface
import ani.saikouc.notifications.TaskScheduler
import ani.saikouc.others.DisabledReports
import ani.saikouc.parsers.AnimeSources
import ani.saikouc.parsers.MangaSources
import ani.saikouc.parsers.NovelSources
import ani.saikouc.parsers.novel.NovelExtensionManager
import ani.saikouc.settings.SettingsActivity
import ani.saikouc.settings.saving.PrefManager
import ani.saikouc.settings.saving.PrefName
import ani.saikouc.util.FinalExceptionHandler
import ani.saikouc.util.Logger
import com.google.android.material.color.DynamicColors
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get


@SuppressLint("StaticFieldLeak")
class App : MultiDexApplication() {
    private lateinit var animeExtensionManager: AnimeExtensionManager
    private lateinit var mangaExtensionManager: MangaExtensionManager
    private lateinit var novelExtensionManager: NovelExtensionManager
    private lateinit var torrentAddonManager: TorrentAddonManager
    private lateinit var downloadAddonManager: DownloadAddonManager

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    init {
        instance = this
    }

    val mFTActivityLifecycleCallbacks = FTActivityLifecycleCallbacks()

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        PrefManager.init(this)

        val crashlytics =
            ani.saikouc.connections.crashlytics.CrashlyticsFactory.createCrashlytics()
        Injekt.addSingletonFactory<CrashlyticsInterface> { crashlytics }
        crashlytics.initialize(this)
        Logger.init(this)
        Thread.setDefaultUncaughtExceptionHandler(FinalExceptionHandler())
        Logger.log(Log.WARN, "App: Logging started")

        Injekt.importModule(AppModule(this))
        Injekt.importModule(PreferenceModule(this))


        val useMaterialYou: Boolean = PrefManager.getVal(PrefName.UseMaterialYou)
        if (useMaterialYou) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        registerActivityLifecycleCallbacks(mFTActivityLifecycleCallbacks)

        crashlytics.setCrashlyticsCollectionEnabled(!DisabledReports)
        (PrefManager.getVal(PrefName.SharedUserID) as Boolean).let {
            if (!it) return@let
            val dUsername = PrefManager.getVal(PrefName.DiscordUserName, null as String?)
            val aUsername = PrefManager.getVal(PrefName.AnilistUserName, null as String?)
            if (dUsername != null) {
                crashlytics.setCustomKey("dUsername", dUsername)
            }
            if (aUsername != null) {
                crashlytics.setCustomKey("aUsername", aUsername)
            }
        }
        crashlytics.setCustomKey("device Info", SettingsActivity.getDeviceInfo())

        initializeNetwork()

        setupNotificationChannels()
        if (!LogcatLogger.isInstalled) {
            LogcatLogger.install(AndroidLogcatLogger(LogPriority.VERBOSE))
        }

        if (PrefManager.getVal<Int>(PrefName.CommentsEnabled) == 0) {
            if (BuildConfig.FLAVOR.contains("fdroid")) {
                PrefManager.setVal(PrefName.CommentsEnabled, 2)
            } else {
                PrefManager.setVal(PrefName.CommentsEnabled, 1)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            animeExtensionManager = Injekt.get()
            animeExtensionManager.findAvailableExtensions()
            Logger.log("Anime Extensions: ${animeExtensionManager.installedExtensionsFlow.first()}")
            AnimeSources.init(animeExtensionManager.installedExtensionsFlow)
        }
        CoroutineScope(Dispatchers.IO).launch {
            mangaExtensionManager = Injekt.get()
            mangaExtensionManager.findAvailableExtensions()
            Logger.log("Manga Extensions: ${mangaExtensionManager.installedExtensionsFlow.first()}")
            MangaSources.init(mangaExtensionManager.installedExtensionsFlow)
        }
        CoroutineScope(Dispatchers.IO).launch {
            novelExtensionManager = Injekt.get()
            novelExtensionManager.findAvailableExtensions()
            Logger.log("Novel Extensions: ${novelExtensionManager.installedExtensionsFlow.first()}")
            NovelSources.init(novelExtensionManager.installedExtensionsFlow)
        }
        GlobalScope.launch {
            torrentAddonManager = Injekt.get()
            downloadAddonManager = Injekt.get()
            torrentAddonManager.init()
            downloadAddonManager.init()
            if (PrefManager.getVal<Int>(PrefName.CommentsEnabled) == 1) {
                CommentsAPI.fetchAuthToken(this@App)
            }

            val useAlarmManager = PrefManager.getVal<Boolean>(PrefName.UseAlarmManager)
            val scheduler = TaskScheduler.create(this@App, useAlarmManager)
            try {
                scheduler.scheduleAllTasks(this@App)
            } catch (e: IllegalStateException) {
                Logger.log("Failed to schedule tasks")
                Logger.log(e)
            }
        }
    }

    private fun setupNotificationChannels() {
        try {
            Notifications.createChannels(this)
        } catch (e: Exception) {
            Logger.log("Failed to modify notification channels")
            Logger.log(e)
        }
    }

    inner class FTActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
        var currentActivity: Activity? = null
        var lastActivity: String? = null
        override fun onActivityCreated(p0: Activity, p1: Bundle?) {
            lastActivity = p0.javaClass.simpleName
        }

        override fun onActivityStarted(p0: Activity) {
            currentActivity = p0
        }

        override fun onActivityResumed(p0: Activity) {
            currentActivity = p0
        }

        override fun onActivityPaused(p0: Activity) {}
        override fun onActivityStopped(p0: Activity) {}
        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
        override fun onActivityDestroyed(p0: Activity) {}
    }

    companion object {
        var instance: App? = null

        /** Reference to the application context.
         *
         * USE WITH EXTREME CAUTION!**/
        var context: Context? = null
        fun currentContext(): Context? {
            return instance?.mFTActivityLifecycleCallbacks?.currentActivity ?: context
        }

        fun currentActivity(): Activity? {
            return instance?.mFTActivityLifecycleCallbacks?.currentActivity
        }
    }
}