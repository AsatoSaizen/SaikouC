package ani.saikouc.settings

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import ani.saikouc.R
import ani.saikouc.connections.anilist.Anilist
import ani.saikouc.databinding.ActivitySettingsCommonBinding
import ani.saikouc.databinding.DialogSetPasswordBinding
import ani.saikouc.databinding.DialogUserAgentBinding
import ani.saikouc.download.DownloadsManager
import ani.saikouc.initActivity
import ani.saikouc.navBarHeight
import ani.saikouc.others.calc.BiometricPromptUtils
import ani.saikouc.restartApp
import ani.saikouc.savePrefsToDownloads
import ani.saikouc.settings.saving.PrefManager
import ani.saikouc.settings.saving.PrefName
import ani.saikouc.settings.saving.internal.Location
import ani.saikouc.settings.saving.internal.PreferenceKeystore
import ani.saikouc.settings.saving.internal.PreferencePackager
import ani.saikouc.statusBarHeight
import ani.saikouc.themes.ThemeManager
import ani.saikouc.toast
import ani.saikouc.util.LauncherWrapper
import ani.saikouc.util.StoragePermissions
import ani.saikouc.util.customAlertDialog
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.UUID


class SettingsCommonActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsCommonBinding
    private lateinit var launcher: LauncherWrapper

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this
        binding = ActivitySettingsCommonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val openDocumentLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    try {
                        val jsonString = contentResolver.openInputStream(uri)?.readBytes()
                            ?: throw Exception("Error reading file")
                        val name = DocumentFile.fromSingleUri(this, uri)?.name ?: "settings"
                        //.sani is encrypted, .ani is not
                        if (name.endsWith(".sani")) {
                            passwordAlertDialog(false) { password ->
                                if (password != null) {
                                    val salt = jsonString.copyOfRange(0, 16)
                                    val encrypted = jsonString.copyOfRange(16, jsonString.size)
                                    val decryptedJson = try {
                                        PreferenceKeystore.decryptWithPassword(
                                            password, encrypted, salt
                                        )
                                    } catch (e: Exception) {
                                        toast(getString(R.string.incorrect_password))
                                        return@passwordAlertDialog
                                    }
                                    if (PreferencePackager.unpack(decryptedJson)) restartApp()
                                } else {
                                    toast(getString(R.string.password_cannot_be_empty))
                                }
                            }
                        } else if (name.endsWith(".ani")) {
                            val decryptedJson = jsonString.toString(Charsets.UTF_8)
                            if (PreferencePackager.unpack(decryptedJson)) restartApp()
                        } else {
                            toast(getString(R.string.unknown_file_type))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast(getString(R.string.error_importing_settings))
                    }
                }
            }
        val contract = ActivityResultContracts.OpenDocumentTree()
        launcher = LauncherWrapper(this, contract)

        binding.apply {

            settingsCommonLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            commonSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            val exDns = listOf(
                "None",
                "Cloudflare",
                "Google",
                "AdGuard",
                "Quad9",
                "AliDNS",
                "DNSPod",
                "360",
                "Quad101",
                "Mullvad",
                "Controld",
                "Njalla",
                "Shecan",
                "Libre"
            )
            settingsExtensionDns.setText(exDns[PrefManager.getVal(PrefName.DohProvider)])
            settingsExtensionDns.setAdapter(
                ArrayAdapter(
                    context, R.layout.item_dropdown, exDns
                )
            )
            settingsExtensionDns.setOnItemClickListener { _, _, i, _ ->
                PrefManager.setVal(PrefName.DohProvider, i)
                settingsExtensionDns.clearFocus()
                restartApp()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = 1,
                        name = getString(R.string.ui_settings),
                        desc = getString(R.string.ui_settings_desc),
                        icon = R.drawable.ic_round_auto_awesome_24,
                        onClick = {
                            startActivity(
                                Intent(
                                    context,
                                    UserInterfaceSettingsActivity::class.java
                                )
                            )
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.open_animanga_directly),
                        desc = getString(R.string.open_animanga_directly_info),
                        icon = R.drawable.ic_round_search_24,
                        isChecked = PrefManager.getVal(PrefName.AniMangaSearchDirect),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.AniMangaSearchDirect, isChecked)
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.download_manager_select),
                        desc = getString(R.string.download_manager_select_desc),
                        icon = R.drawable.ic_download_24,
                        onClick = {
                            val managers = arrayOf("Default", "1DM", "ADM")
                            customAlertDialog().apply {
                                setTitle(getString(R.string.download_manager))
                                singleChoiceItems(
                                    managers,
                                    PrefManager.getVal(PrefName.DownloadManager)
                                ) { count ->
                                    PrefManager.setVal(PrefName.DownloadManager, count)
                                }
                                show()
                            }
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.app_lock),
                        desc = getString(R.string.app_lock_desc),
                        icon = R.drawable.ic_round_lock_open_24,
                        onClick = {
                            customAlertDialog().apply {
                                val view = DialogSetPasswordBinding.inflate(layoutInflater)
                                setTitle(R.string.app_lock)
                                setCustomView(view.root)
                                setPosButton(R.string.ok) {
                                    if (view.forgotPasswordCheckbox.isChecked) {
                                        PrefManager.setVal(PrefName.OverridePassword, true)
                                    }
                                    val password = view.passwordInput.text.toString()
                                    val confirmPassword = view.confirmPasswordInput.text.toString()
                                    if (password == confirmPassword && password.isNotEmpty()) {
                                        PrefManager.setVal(PrefName.AppPassword, password)
                                        if (view.biometricCheckbox.isChecked) {
                                            val canBiometricPrompt =
                                                BiometricManager.from(applicationContext)
                                                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

                                            if (canBiometricPrompt) {
                                                val biometricPrompt =
                                                    BiometricPromptUtils.createBiometricPrompt(this@SettingsCommonActivity) { _ ->
                                                        val token = UUID.randomUUID().toString()
                                                        PrefManager.setVal(
                                                            PrefName.BiometricToken,
                                                            token
                                                        )
                                                        toast(R.string.success)
                                                    }
                                                val promptInfo =
                                                    BiometricPromptUtils.createPromptInfo(this@SettingsCommonActivity)
                                                biometricPrompt.authenticate(promptInfo)
                                            }

                                        } else {
                                            PrefManager.setVal(PrefName.BiometricToken, "")
                                            toast(R.string.success)
                                        }
                                    } else {
                                        toast(R.string.password_mismatch)
                                    }
                                }
                                setNegButton(R.string.cancel)
                                setNeutralButton(R.string.remove) {
                                    PrefManager.setVal(PrefName.AppPassword, "")
                                    PrefManager.setVal(PrefName.BiometricToken, "")
                                    PrefManager.setVal(PrefName.OverridePassword, false)
                                    toast(R.string.success)
                                }
                                setOnShowListener {
                                    view.passwordInput.requestFocus()
                                    val canAuthenticate =
                                        BiometricManager.from(applicationContext).canAuthenticate(
                                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                                        ) == BiometricManager.BIOMETRIC_SUCCESS
                                    view.biometricCheckbox.isVisible = canAuthenticate
                                    view.biometricCheckbox.isChecked =
                                        PrefManager.getVal(PrefName.BiometricToken, "").isNotEmpty()
                                    view.forgotPasswordCheckbox.isChecked =
                                        PrefManager.getVal(PrefName.OverridePassword)
                                }
                                show()
                            }
                        }

                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.backup_restore),
                        desc = getString(R.string.backup_restore_desc),
                        icon = R.drawable.backup_restore,
                        onClick = {
                            StoragePermissions.downloadsPermission(context)
                            val selectedArray = mutableListOf(false)
                            val filteredLocations = Location.entries.filter { it.exportable }
                            selectedArray.addAll(List(filteredLocations.size - 1) { false })
                            val dialog = AlertDialog.Builder(context, R.style.MyPopup)
                                .setTitle(R.string.backup_restore).setMultiChoiceItems(
                                    filteredLocations.map { it.name }.toTypedArray(),
                                    selectedArray.toBooleanArray()
                                ) { _, which, isChecked ->
                                    selectedArray[which] = isChecked
                                }.setPositiveButton(R.string.button_restore) { dialog, _ ->
                                    openDocumentLauncher.launch(arrayOf("*/*"))
                                    dialog.dismiss()
                                }.setNegativeButton(R.string.button_backup) { dialog, _ ->
                                    if (!selectedArray.contains(true)) {
                                        toast(R.string.no_location_selected)
                                        return@setNegativeButton
                                    }
                                    dialog.dismiss()
                                    val selected =
                                        filteredLocations.filterIndexed { index, _ -> selectedArray[index] }
                                    if (selected.contains(Location.Protected)) {
                                        passwordAlertDialog(true) { password ->
                                            if (password != null) {
                                                savePrefsToDownloads(
                                                    "DantotsuSettings",
                                                    PrefManager.exportAllPrefs(selected),
                                                    context,
                                                    password
                                                )
                                            } else {
                                                toast(R.string.password_cannot_be_empty)
                                            }
                                        }
                                    } else {
                                        savePrefsToDownloads(
                                            "DantotsuSettings",
                                            PrefManager.exportAllPrefs(selected),
                                            context,
                                            null
                                        )
                                    }
                                }.setNeutralButton(R.string.cancel) { dialog, _ ->
                                    dialog.dismiss()
                                }.create()
                            dialog.window?.setDimAmount(0.8f)
                            dialog.show()
                        },
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.change_download_location),
                        desc = getString(R.string.change_download_location_desc),
                        icon = R.drawable.ic_round_source_24,
                        onClick = {
                            context.customAlertDialog().apply {
                                setTitle(R.string.change_download_location)
                                setMessage(R.string.download_location_msg)
                                setPosButton(R.string.ok) {
                                    val oldUri = PrefManager.getVal<String>(PrefName.DownloadsDir)
                                    launcher.registerForCallback { success ->
                                        if (success) {
                                            toast(getString(R.string.please_wait))
                                            val newUri =
                                                PrefManager.getVal<String>(PrefName.DownloadsDir)
                                            GlobalScope.launch(Dispatchers.IO) {
                                                Injekt.get<DownloadsManager>().moveDownloadsDir(
                                                    context, Uri.parse(oldUri), Uri.parse(newUri)
                                                ) { finished, message ->
                                                    if (finished) {
                                                        toast(getString(R.string.success))
                                                    } else {
                                                        toast(message)
                                                    }
                                                }
                                            }
                                        } else {
                                            toast(getString(R.string.error))
                                        }
                                    }
                                    launcher.launch()
                                }
                                setNegButton(R.string.cancel)
                                show()
                            }
                        }
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.always_continue_content),
                        desc = getString(R.string.always_continue_content_desc),
                        icon = R.drawable.ic_round_delete_24,
                        isChecked = PrefManager.getVal(PrefName.ContinueMedia),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.ContinueMedia, isChecked)
                        }
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.hide_private),
                        desc = getString(R.string.hide_private_desc),
                        icon = R.drawable.ic_round_remove_red_eye_24,
                        isChecked = PrefManager.getVal(PrefName.HidePrivate),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.HidePrivate, isChecked)
                            restartApp()
                        }
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.search_source_list),
                        desc = getString(R.string.search_source_list_desc),
                        icon = R.drawable.ic_round_search_sources_24,
                        isChecked = PrefManager.getVal(PrefName.SearchSources),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.SearchSources, isChecked)
                        }
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.recentlyListOnly),
                        desc = getString(R.string.recentlyListOnly_desc),
                        icon = R.drawable.ic_round_new_releases_24,
                        isChecked = PrefManager.getVal(PrefName.RecentlyListOnly),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.RecentlyListOnly, isChecked)
                        }
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.adult_only_content),
                        desc = getString(R.string.adult_only_content_desc),
                        icon = R.drawable.ic_round_nsfw_24,
                        isChecked = PrefManager.getVal(PrefName.AdultOnly),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.AdultOnly, isChecked)
                            restartApp()
                        },
                        isVisible = Anilist.adult

                    ),
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }
            var previousStart: View = when (PrefManager.getVal<Int>(PrefName.DefaultStartUpTab)) {
                0 -> uiSettingsAnime
                1 -> uiSettingsHome
                2 -> uiSettingsManga
                else -> uiSettingsHome
            }
            previousStart.alpha = 1f
            fun uiDefault(mode: Int, current: View) {
                previousStart.alpha = 0.33f
                previousStart = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.DefaultStartUpTab, mode)
                initActivity(context)
            }

            uiSettingsAnime.setOnClickListener {
                uiDefault(0, it)
            }

            uiSettingsHome.setOnClickListener {
                uiDefault(1, it)
            }

            uiSettingsManga.setOnClickListener {
                uiDefault(2, it)
            }

        }
    }

    private fun passwordAlertDialog(isExporting: Boolean, callback: (CharArray?) -> Unit) {
        val password = CharArray(16).apply { fill('0') }

        // Inflate the dialog layout
        val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
        val box = dialogView.userAgentTextBox
        box.hint = getString(R.string.password)
        box.setSingleLine()

        val dialog =
            AlertDialog.Builder(this, R.style.MyPopup).setTitle(getString(R.string.enter_password))
                .setView(dialogView.root)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    password.fill('0')
                    dialog.dismiss()
                    callback(null)
                }.create()

        fun handleOkAction() {
            val editText = dialogView.userAgentTextBox
            if (editText.text?.isNotBlank() == true) {
                editText.text?.toString()?.trim()?.toCharArray(password)
                dialog.dismiss()
                callback(password)
            } else {
                toast(getString(R.string.password_cannot_be_empty))
            }
        }
        box.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleOkAction()
                true
            } else {
                false
            }
        }
        dialogView.subtitle.visibility = View.VISIBLE
        if (!isExporting) dialogView.subtitle.text =
            getString(R.string.enter_password_to_decrypt_file)


        dialog.window?.setDimAmount(0.8f)
        dialog.show()

        // Override the positive button here
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            handleOkAction()
        }

    }

}