package ani.saikouc.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ani.saikouc.BottomSheetDialogFragment
import ani.saikouc.databinding.BottomSheetProxyBinding
import ani.saikouc.restartApp
import ani.saikouc.settings.saving.PrefManager
import ani.saikouc.settings.saving.PrefName

class ProxyDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetProxyBinding? = null
    private val binding get() = _binding!!

    private var proxyHost: String? = PrefManager.getVal<String>(PrefName.Socks5ProxyHost).orEmpty()
    private var proxyPort: String? = PrefManager.getVal<String>(PrefName.Socks5ProxyPort).orEmpty()
    private var proxyUsername: String? =
        PrefManager.getVal<String>(PrefName.Socks5ProxyUsername).orEmpty()
    private var proxyPassword: String? =
        PrefManager.getVal<String>(PrefName.Socks5ProxyPassword).orEmpty()
    private var authEnabled: Boolean = PrefManager.getVal<Boolean>(PrefName.ProxyAuthEnabled)
    private val proxyEnabled: Boolean = PrefManager.getVal<Boolean>(PrefName.EnableSocks5Proxy)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetProxyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.proxyHost.setText(proxyHost)
        binding.proxyPort.setText(proxyPort)
        binding.proxyUsername.setText(proxyUsername)
        binding.proxyPassword.setText(proxyPassword)
        binding.proxyAuthentication.isChecked = authEnabled

        toggleAuthentication(authEnabled)

        binding.proxySave.setOnClickListener {
            proxyHost = binding.proxyHost.text?.toString().orEmpty()
            proxyPort = binding.proxyPort.text?.toString().orEmpty()
            proxyUsername = binding.proxyUsername.text?.toString().orEmpty()
            proxyPassword = binding.proxyPassword.text?.toString().orEmpty()

            PrefManager.setVal(PrefName.Socks5ProxyHost, proxyHost)
            PrefManager.setVal(PrefName.Socks5ProxyPort, proxyPort)
            PrefManager.setVal(PrefName.Socks5ProxyUsername, proxyUsername)
            PrefManager.setVal(PrefName.Socks5ProxyPassword, proxyPassword)

            dismiss()
            if (proxyEnabled) activity?.restartApp()
        }

        binding.proxyAuthentication.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ProxyAuthEnabled, isChecked)
            toggleAuthentication(isChecked)
        }
    }

    private fun toggleAuthentication(isChecked: Boolean) {
        arrayOf(
            binding.proxyUsername,
            binding.proxyPassword,
            binding.proxyUsernameLayout,
            binding.proxyPasswordLayout
        ).forEach {
            it.isEnabled = isChecked
            it.alpha = when (isChecked) {
                true -> 1f
                false -> 0.5f
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
