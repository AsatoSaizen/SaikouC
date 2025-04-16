package ani.saikouc.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ani.saikouc.BottomSheetDialogFragment
import ani.saikouc.R
import ani.saikouc.databinding.BottomSheetDiscordRpcBinding
import ani.saikouc.settings.saving.PrefManager
import ani.saikouc.settings.saving.PrefName

class DiscordDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetDiscordRpcBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDiscordRpcBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (PrefManager.getCustomVal("discord_mode", "saikouc")) {
            "nothing" -> binding.radioNothing.isChecked = true
            "saikouc" -> binding.radioDantotsu.isChecked = true
            "anilist" -> binding.radioAnilist.isChecked = true
            else -> binding.radioAnilist.isChecked = true
        }
        binding.showIcon.isChecked = PrefManager.getVal(PrefName.ShowAniListIcon)
        binding.showIcon.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ShowAniListIcon, isChecked)
        }
        binding.anilistLinkPreview.text =
            getString(R.string.anilist_link, PrefManager.getVal<String>(PrefName.AnilistUserName))

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                binding.radioNothing.id -> "nothing"
                binding.radioDantotsu.id -> "saikouc"
                binding.radioAnilist.id -> "anilist"
                else -> "saikouc"
            }
            PrefManager.setCustomVal("discord_mode", mode)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}