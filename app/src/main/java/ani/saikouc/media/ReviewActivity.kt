package ani.saikouc.media

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.saikouc.R
import ani.saikouc.connections.anilist.Anilist
import ani.saikouc.connections.anilist.api.Query
import ani.saikouc.databinding.ActivityFollowBinding
import ani.saikouc.initActivity
import ani.saikouc.navBarHeight
import ani.saikouc.statusBarHeight
import ani.saikouc.themes.ThemeManager
import ani.saikouc.util.ActivityMarkdownCreator
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding
    val adapter = GroupieAdapter()
    private val reviews = mutableListOf<Query.Review>()
    var mediaId = 0
    private var currentPage: Int = 1
    private var hasNextPage: Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
        }
        binding.listFrameLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        setContentView(binding.root)
        mediaId = intent.getIntExtra("mediaId", -1)
        if (mediaId == -1) {
            finish()
            return
        }
        binding.followerGrid.visibility = View.GONE
        binding.followerList.visibility = View.GONE
        binding.followFilterButton.setImageResource(R.drawable.ic_add)
        binding.followFilterButton.setOnClickListener {
            ContextCompat.startActivity(
                this,
                Intent(this, ActivityMarkdownCreator::class.java)
                    .putExtra("type", "review"),
                null
            )
        }
        binding.followFilterButton.visibility = View.GONE
        binding.listTitle.text = getString(R.string.reviews)
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.listProgressBar.visibility = View.VISIBLE
        binding.listBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        lifecycleScope.launch(Dispatchers.IO) {
            val response = Anilist.query.getReviews(mediaId)?.data?.page
            withContext(Dispatchers.Main) {
                binding.listProgressBar.visibility = View.GONE
                binding.listRecyclerView.setOnTouchListener { _, event ->
                    if (event?.action == MotionEvent.ACTION_UP) {
                        if (hasNextPage && !binding.listRecyclerView.canScrollVertically(1) && !binding.followRefresh.isVisible
                            && binding.listRecyclerView.adapter!!.itemCount != 0 &&
                            (binding.listRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() == (binding.listRecyclerView.adapter!!.itemCount - 1)
                        ) {
                            binding.followRefresh.visibility = ViewGroup.VISIBLE
                            loadPage(++currentPage) {
                                binding.followRefresh.visibility = ViewGroup.GONE
                            }
                        }
                    }
                    false
                }
                currentPage = response?.pageInfo?.currentPage ?: 1
                hasNextPage = response?.pageInfo?.hasNextPage ?: false
                response?.reviews?.let {
                    reviews.addAll(it)
                    fillList()
                }
            }
        }
    }

    private fun loadPage(page: Int, callback: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val response = Anilist.query.getReviews(mediaId, page)
            currentPage = response?.data?.page?.pageInfo?.currentPage ?: 1
            hasNextPage = response?.data?.page?.pageInfo?.hasNextPage ?: false
            withContext(Dispatchers.Main) {
                response?.data?.page?.reviews?.let {
                    reviews.addAll(it)
                    fillList()
                }
                callback()
            }
        }
    }

    private fun fillList() {
        adapter.clear()
        reviews.forEach {
            adapter.add(ReviewAdapter(it))
        }
    }
}