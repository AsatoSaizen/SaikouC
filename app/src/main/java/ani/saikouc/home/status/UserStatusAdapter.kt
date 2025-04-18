package ani.saikouc.home.status

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ani.saikouc.R
import ani.saikouc.connections.anilist.Anilist
import ani.saikouc.databinding.ItemUserStatusBinding
import ani.saikouc.getAppString
import ani.saikouc.loadImage
import ani.saikouc.profile.ProfileActivity
import ani.saikouc.profile.User
import ani.saikouc.setAnimation
import ani.saikouc.settings.saving.PrefManager
import ani.saikouc.snackString
import ani.saikouc.util.ActivityMarkdownCreator

class UserStatusAdapter(private val user: ArrayList<User>) :
    RecyclerView.Adapter<UserStatusAdapter.UsersViewHolder>() {

    inner class UsersViewHolder(val binding: ItemUserStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (user[bindingAdapterPosition].activity.isEmpty()) {
                    snackString("No activity")
                    return@setOnClickListener
                }
                StatusActivity.user = user
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(
                        itemView.context,
                        StatusActivity::class.java
                    ).putExtra("position", bindingAdapterPosition),
                    null
                )
            }
            itemView.setOnLongClickListener {
                if (user[bindingAdapterPosition].id == Anilist.userid) {
                    ContextCompat.startActivity(
                        itemView.context,
                        Intent(itemView.context, ActivityMarkdownCreator::class.java)
                            .putExtra("type", "activity"),
                        null
                    )
                } else {
                    ContextCompat.startActivity(
                        itemView.context,
                        Intent(
                            itemView.context,
                            ProfileActivity::class.java
                        ).putExtra("userId", user[bindingAdapterPosition].id),
                        null
                    )
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        return UsersViewHolder(
            ItemUserStatusBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val b = holder.binding
        setAnimation(b.root.context, b.root)
        val user = user[position]
        b.profileUserAvatar.loadImage(user.pfp)
        b.profileUserName.text =
            if (Anilist.userid == user.id) getAppString(R.string.your_story) else user.name
        val watchedActivity = PrefManager.getCustomVal<Set<Int>>("activities", setOf())
        val booleanList = user.activity.map { watchedActivity.contains(it.id) }
        b.profileUserStatusIndicator.setParts(
            user.activity.size,
            booleanList,
            user.id == Anilist.userid
        )

    }

    override fun getItemCount(): Int = user.size
}
