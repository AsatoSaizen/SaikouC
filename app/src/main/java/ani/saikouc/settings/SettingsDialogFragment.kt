package ani.saikouc.settings

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ani.saikouc.BottomSheetDialogFragment
import ani.saikouc.MainActivity
import ani.saikouc.R
import ani.saikouc.connections.anilist.Anilist
import ani.saikouc.databinding.BottomSheetSettingsBinding
import ani.saikouc.download.anime.OfflineAnimeFragment
import ani.saikouc.download.manga.OfflineMangaFragment
import ani.saikouc.getThemeColor
import ani.saikouc.home.AnimeFragment
import ani.saikouc.home.HomeFragment
import ani.saikouc.home.LoginFragment
import ani.saikouc.home.MangaFragment
import ani.saikouc.home.NoInternet
import ani.saikouc.incognitoNotification
import ani.saikouc.loadImage
import ani.saikouc.offline.OfflineFragment
import ani.saikouc.profile.ProfileActivity
import ani.saikouc.profile.activity.FeedActivity
import ani.saikouc.profile.notification.NotificationActivity
import ani.saikouc.setSafeOnClickListener
import ani.saikouc.settings.saving.PrefManager
import ani.saikouc.settings.saving.PrefName
import ani.saikouc.startMainActivity
import ani.saikouc.util.customAlertDialog
import eu.kanade.tachiyomi.util.system.getSerializableCompat
import java.util.Timer
import kotlin.concurrent.schedule

class SettingsDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var pageType: PageType
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageType = arguments?.getSerializableCompat("pageType") as? PageType ?: PageType.HOME
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = dialog?.window
        window?.statusBarColor = Color.CYAN
        window?.navigationBarColor =
            requireContext().getThemeColor(com.google.android.material.R.attr.colorSurface)
        val notificationIcon = if (Anilist.unreadNotificationCount > 0) {
            R.drawable.ic_round_notifications_active_24
        } else {
            R.drawable.ic_round_notifications_none_24
        }
        binding.settingsNotification.setImageResource(notificationIcon)

        if (Anilist.token != null) {
            binding.settingsLogin.setText(R.string.logout)
            binding.settingsLogin.setOnClickListener {
                requireContext().customAlertDialog().apply {
                    setTitle(R.string.logout)
                    setMessage(R.string.logout_confirm)
                    setPosButton(R.string.yes) {
                        Anilist.removeSavedToken()
                        startMainActivity(requireActivity())
                    }
                    setNegButton(R.string.no)
                    show()
                }
            }
            binding.settingsUsername.text = Anilist.username
            binding.settingsUserAvatar.loadImage(Anilist.avatar)
        } else {
            binding.settingsUsername.visibility = View.GONE
            binding.settingsLogin.setText(R.string.login)
            binding.settingsLogin.setOnClickListener {
                dismiss()
                Anilist.loginIntent(requireActivity())
            }
        }
        binding.settingsNotificationCount.isVisible = Anilist.unreadNotificationCount > 0
        binding.settingsNotificationCount.text = Anilist.unreadNotificationCount.toString()
        binding.settingsUserAvatar.setOnClickListener {
            ContextCompat.startActivity(
                requireContext(), Intent(requireContext(), ProfileActivity::class.java)
                    .putExtra("userId", Anilist.userid), null
            )
        }

        binding.settingsIncognito.isChecked = PrefManager.getVal(PrefName.Incognito)
        binding.settingsIncognito.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.Incognito, isChecked)
            incognitoNotification(requireContext())
        }

        binding.settingsExtensionSettings.setSafeOnClickListener {
            startActivity(Intent(activity, ExtensionsActivity::class.java))
            dismiss()
        }

        binding.settingsSettings.setSafeOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
            dismiss()
        }

        binding.settingsActivity.setSafeOnClickListener {
            startActivity(Intent(activity, FeedActivity::class.java))
            dismiss()
        }

        binding.settingsNotification.setOnClickListener {
            startActivity(Intent(activity, NotificationActivity::class.java))
            dismiss()
        }
        binding.settingsDownloads.isChecked = PrefManager.getVal(PrefName.OfflineMode)
        binding.settingsDownloads.setOnCheckedChangeListener { _, isChecked ->
            Timer().schedule(300) {
                when (pageType) {
                    PageType.MANGA -> {
                        val intent = Intent(activity, NoInternet::class.java)
                        intent.putExtra(
                            "FRAGMENT_CLASS_NAME",
                            OfflineMangaFragment::class.java.name
                        )
                        startActivity(intent)
                    }

                    PageType.ANIME -> {
                        val intent = Intent(activity, NoInternet::class.java)
                        intent.putExtra(
                            "FRAGMENT_CLASS_NAME",
                            OfflineAnimeFragment::class.java.name
                        )
                        startActivity(intent)
                    }

                    PageType.HOME -> {
                        val intent = Intent(activity, NoInternet::class.java)
                        intent.putExtra("FRAGMENT_CLASS_NAME", OfflineFragment::class.java.name)
                        startActivity(intent)
                    }

                    PageType.OfflineMANGA -> {
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.putExtra("FRAGMENT_CLASS_NAME", MangaFragment::class.java.name)
                        startActivity(intent)
                    }

                    PageType.OfflineHOME -> {
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.putExtra(
                            "FRAGMENT_CLASS_NAME",
                            if (Anilist.token != null) HomeFragment::class.java.name else LoginFragment::class.java.name
                        )
                        startActivity(intent)
                    }

                    PageType.OfflineANIME -> {
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.putExtra("FRAGMENT_CLASS_NAME", AnimeFragment::class.java.name)
                        startActivity(intent)
                    }
                }

                dismiss()
                PrefManager.setVal(PrefName.OfflineMode, isChecked)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        enum class PageType {
            MANGA, ANIME, HOME, OfflineMANGA, OfflineANIME, OfflineHOME
        }

        fun newInstance(pageType: PageType): SettingsDialogFragment {
            val fragment = SettingsDialogFragment()
            val args = Bundle()
            args.putSerializable("pageType", pageType)
            fragment.arguments = args
            return fragment
        }
    }
}
