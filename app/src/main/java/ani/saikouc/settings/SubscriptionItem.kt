package ani.saikouc.settings

import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import ani.saikouc.R
import ani.saikouc.databinding.ItemSubscriptionBinding
import ani.saikouc.loadImage
import ani.saikouc.media.MediaDetailsActivity
import ani.saikouc.notifications.subscription.SubscriptionHelper
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem

class SubscriptionItem(
    val id: Int,
    private val media: SubscriptionHelper.Companion.SubscribeMedia,
    private val adapter: GroupieAdapter,
    private val onItemRemoved: (Int) -> Unit
) : BindableItem<ItemSubscriptionBinding>() {
    private lateinit var binding: ItemSubscriptionBinding

    override fun bind(viewBinding: ItemSubscriptionBinding, position: Int) {
        binding = viewBinding
        val context = binding.root.context

        binding.subscriptionName.text = media.name
        binding.root.setOnClickListener {
            ContextCompat.startActivity(
                context,
                Intent(context, MediaDetailsActivity::class.java).putExtra("mediaId", media.id),
                null
            )
        }
        binding.subscriptionCover.loadImage(media.image)
        binding.deleteSubscription.setOnClickListener {
            SubscriptionHelper.deleteSubscription(id, true)
            adapter.remove(this)
            onItemRemoved(id)
        }
    }

    override fun getLayout(): Int = R.layout.item_subscription

    override fun initializeViewBinding(view: View): ItemSubscriptionBinding =
        ItemSubscriptionBinding.bind(view)
}