package br.com.wakim.eslpodclient.podcastlist.view

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import br.com.wakim.eslpodclient.R
import br.com.wakim.eslpodclient.dagger.PodcastPlayerComponent
import br.com.wakim.eslpodclient.extensions.dp
import br.com.wakim.eslpodclient.extensions.makeVisible
import br.com.wakim.eslpodclient.model.PodcastItem
import br.com.wakim.eslpodclient.podcastlist.adapter.PodcastListAdapter
import br.com.wakim.eslpodclient.podcastlist.presenter.PodcastListPresenter
import br.com.wakim.eslpodclient.podcastplayer.view.ListPlayerView
import br.com.wakim.eslpodclient.view.BaseActivity
import br.com.wakim.eslpodclient.view.BasePresenterFragment
import br.com.wakim.eslpodclient.widget.BottomSpacingItemDecoration
import br.com.wakim.eslpodclient.widget.SpacingItemDecoration
import butterknife.bindView
import java.util.*
import javax.inject.Inject

open class PodcastListFragment: BasePresenterFragment<PodcastListPresenter>(), PodcastListView {

    override var hasMore: Boolean = false

    val recyclerView: RecyclerView by bindView(R.id.recycler_view)
    val swipeRefresh: SwipeRefreshLayout by bindView(R.id.swipe_refresh)

    val bottomSpacingDecoration = BottomSpacingItemDecoration(0)

    lateinit var adapter: PodcastListAdapter

    @Inject
    lateinit var baseActivity: BaseActivity

    @Inject
    lateinit var playerView: ListPlayerView

    @Inject
    fun injectPresenter(presenter: PodcastListPresenter) {
        presenter.view = this
        this.presenter = presenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        inject()
        super.onCreate(savedInstanceState)
    }

    open fun inject() =
            (context.getSystemService(PodcastPlayerComponent::class.java.simpleName) as PodcastPlayerComponent).inject(this)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater?.inflate(R.layout.fragment_podcastlist, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        configureAdapter()
        configureRecyclerView()

        swipeRefresh.apply {
            this.setOnRefreshListener {
                adapter.removeAll()
                presenter.onRefresh()

                isRefreshing = false
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    fun share(podcastItem: PodcastItem) {
        presenter.share(podcastItem)
    }

    fun favorite(podcastItem: PodcastItem) {
        presenter.favorite(podcastItem)
    }

    fun download(podcastItem: PodcastItem) {
        presenter.download(podcastItem)
    }

    fun openWith(podcastItem: PodcastItem) {
        presenter.openWith(podcastItem)
    }

    fun configureAdapter() {
        adapter = PodcastListAdapter(context)
        adapter.clickListener = { podcastItem ->
            showPlayerViewIfNeeded()
            playerView.play(podcastItem)
        }

        adapter.overflowMenuClickListener = { podcastItem, anchor ->
            showPopupMenuFor(podcastItem, anchor)
        }
    }

    fun configureRecyclerView() {
        val hSpacing = dp(4)
        val vSpacing = dp(2)

        recyclerView.adapter = adapter

        recyclerView.addItemDecoration(SpacingItemDecoration(hSpacing, vSpacing, hSpacing, vSpacing))
        recyclerView.addItemDecoration(bottomSpacingDecoration)

        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val linearLayoutManager : LinearLayoutManager = recyclerView?.layoutManager as LinearLayoutManager

                val totalItemCount = linearLayoutManager.itemCount
                val lastVisible = linearLayoutManager.findLastVisibleItemPosition()

                val mustLoadMore = totalItemCount <= (lastVisible + PodcastListActivity.MINIMUM_THRESHOLD)

                if (mustLoadMore && hasMore && !adapter.loading) {
                    presenter.loadNextPage()
                }
            }
        })
    }

    open fun showPopupMenuFor(podcastItem: PodcastItem, anchor: View) {
        val popupMenu = PopupMenu(context, anchor)

        popupMenu.inflate(R.menu.podcast_item_menu)

        popupMenu.setOnMenuItemClickListener { menu ->
            when (menu.itemId) {
                R.id.share     -> share(podcastItem)
                R.id.favorite  -> favorite(podcastItem)
                R.id.download  -> download(podcastItem)
                R.id.open_with -> openWith(podcastItem)
            }

            true
        }

        popupMenu.show()
    }

    fun showPlayerViewIfNeeded() {
        if (playerView.isVisible()) {
            return
        }

        playerView.makeVisible()

        bottomSpacingDecoration.bottomSpacing = dp(72)
        recyclerView.invalidateItemDecorations()
    }

    override fun addItems(list: ArrayList<PodcastItem>) {
        adapter.addAll(list)
    }

    override fun setItems(list: ArrayList<PodcastItem>) {
        adapter.setItems(list)
    }

    override fun addItem(podcastItem: PodcastItem) {
        adapter.add(podcastItem)
    }

    override fun remove(podcastItem: PodcastItem) {
        adapter.remove(podcastItem)
    }

    override fun setLoading(loading: Boolean) {
        adapter.loading = loading
    }

    override fun showMessage(messageResId: Int): Snackbar = baseActivity.showMessage(messageResId)

    override fun showMessage(messageResId: Int, action: String, clickListener: (() -> Unit)?) =
            baseActivity.showMessage(messageResId, action, clickListener)

    fun isSwipeRefreshEnabled() = if (view == null) false else swipeRefresh.isEnabled

    fun setSwipeRefreshEnabled(enabled: Boolean) {
        if (view != null) {
            swipeRefresh.isEnabled = enabled
        }
    }
}