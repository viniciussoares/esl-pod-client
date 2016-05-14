package br.com.wakim.eslpodclient.podcastlist.presenter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ShareCompat
import br.com.wakim.eslpodclient.Application
import br.com.wakim.eslpodclient.BuildConfig
import br.com.wakim.eslpodclient.R
import br.com.wakim.eslpodclient.extensions.ofIOToMainThread
import br.com.wakim.eslpodclient.extensions.startActivity
import br.com.wakim.eslpodclient.extensions.view
import br.com.wakim.eslpodclient.interactor.FavoritedPodcastItemInteractor
import br.com.wakim.eslpodclient.interactor.PodcastInteractor
import br.com.wakim.eslpodclient.interactor.StorageInteractor
import br.com.wakim.eslpodclient.model.DownloadStatus
import br.com.wakim.eslpodclient.model.PodcastItem
import br.com.wakim.eslpodclient.podcastlist.view.PodcastListView
import br.com.wakim.eslpodclient.presenter.Presenter
import br.com.wakim.eslpodclient.receiver.ConnectivityException
import br.com.wakim.eslpodclient.rx.ConnectivityPublishSubject
import br.com.wakim.eslpodclient.rx.PermissionPublishSubject
import br.com.wakim.eslpodclient.service.PlaylistManager
import br.com.wakim.eslpodclient.view.PermissionRequester
import rx.android.schedulers.AndroidSchedulers
import java.util.*

open class PodcastListPresenter(private   val app: Application,
                                private   val interactor: PodcastInteractor,
                                private   val permissionRequester: PermissionRequester,
                                private   val playlistManager: PlaylistManager,
                                protected val storageInteractor: StorageInteractor,
                                private   val favoritedPodcastItemInteractor: FavoritedPodcastItemInteractor,
                                private   val baseActivity: Activity) : Presenter<PodcastListView>() {

    companion object {
        private final val ITEMS_EXTRA = "ITEMS"
        private final val NEXT_PAGE_TOKEN_EXTRA = "NEXT_PAGE_TOKEN"
        private final val DOWNLOAD_PODCAST_EXTRA = "DOWNLOAD_PODCAST"
    }

    var items : ArrayList<PodcastItem> = ArrayList()

    var nextPageToken : String? = null

    var downloadPodcastItem: PodcastItem? = null

    override fun onViewCreated(savedInstanceState : Bundle?) {
        savedInstanceState?.let {
            items = it.getParcelableArrayList(ITEMS_EXTRA)
            nextPageToken = it.getString(NEXT_PAGE_TOKEN_EXTRA)
            downloadPodcastItem = it.getParcelable(DOWNLOAD_PODCAST_EXTRA)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ITEMS_EXTRA, items)
        outState.putString(NEXT_PAGE_TOKEN_EXTRA, nextPageToken)
        outState.putParcelable(DOWNLOAD_PODCAST_EXTRA, downloadPodcastItem)
    }

    override fun onStart() {
        super.onStart()

        if (items.isNotEmpty()) {
            playlistManager.setItems(items)
        }

        loadFirstPageIfNeeded()

        addSubscription {
            PermissionPublishSubject
                    .INSTANCE
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe { permission ->
                        if (permission.requestCode == Application.LIST_DOWNLOAD_WRITE_STORAGE_PERMISSION) {
                            if (downloadPodcastItem != null && permission.isGranted()) {
                                download(downloadPodcastItem!!)
                            } else {
                                view!!.showMessage(R.string.write_external_storage_permission_needed_to_download)
                            }
                        }

                        if (permission.requestCode == Application.LIST_REMOVE_DOWNLOAD_WRITE_STORAGE_PERMISSION) {
                            if (downloadPodcastItem != null && permission.isGranted()) {
                                removeDownload(downloadPodcastItem!!)
                            } else {
                                view!!.showMessage(R.string.write_external_storage_permission_needed_to_remove_download)
                            }
                        }
                    }
        }

        addSubscription {
            ConnectivityPublishSubject
                    .INSTANCE
                    .subscribe { connected ->
                        if (connected) {
                            loadFirstPageIfNeeded()
                        }
                    }
        }
    }

    override fun onResume() {
        super.onResume()
        playlistManager.setItems(items)
    }

    override fun onStop() {
        super.onStop()
    }

    fun loadFirstPageIfNeeded() {
        if (items.isEmpty()) {
            loadNextPage()
        } else {
            view?.let {
                it.setItems(items)
                it.hasMore = nextPageToken != null
            }
        }
    }

    fun loadNextPage() {
        view!!.setLoading(true)

        addSubscription {
            interactor.getPodcasts(nextPageToken)
                    .ofIOToMainThread()
                    .subscribe (
                            { podcastList ->
                                items.addAll(podcastList.list)
                                nextPageToken = podcastList.nextPageToken

                                view?.let {
                                    it.setLoading(false)
                                    it.addItems(podcastList.list)

                                    it.hasMore = podcastList.nextPageToken != null

                                    playlistManager.setItems(items)
                                }
                            },
                            { e: Throwable ->
                                if (e is ConnectivityException) {
                                    view?.showMessage(R.string.no_connectivity)
                                }

                                view?.setLoading(false)
                            }
                    )
        }
    }

    fun onRefresh() {
        items.clear()
        playlistManager.clearItems()

        nextPageToken = null

        loadNextPage()
    }

    fun download(podcastItem: PodcastItem) {
        if (!hasPermission(app, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissionRequester.requestPermissions(Application.LIST_DOWNLOAD_WRITE_STORAGE_PERMISSION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

            downloadPodcastItem = podcastItem

            return
        }

        val downloadStatus = storageInteractor.startDownloadIfNeeded(podcastItem)

        addSubscription {
            downloadStatus.ofIOToMainThread()
            .subscribe{ downloadStatus ->
                when (downloadStatus.status) {
                    DownloadStatus.DOWNLOADED -> view?.showMessage(R.string.podcast_already_downloaded)
                    DownloadStatus.DOWNLOADING -> view?.showMessage(R.string.podcast_download_started, app.getString(R.string.cancel)) {
                        storageInteractor.cancelDownload(downloadStatus)
                    }
                }
            }
        }
    }

    fun removeDownload(podcastItem: PodcastItem) {
        if (!hasPermission(app, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissionRequester.requestPermissions(Application.LIST_REMOVE_DOWNLOAD_WRITE_STORAGE_PERMISSION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

            downloadPodcastItem = podcastItem

            return
        }

        val downloadStatus = storageInteractor.startDownloadIfNeeded(podcastItem)

        addSubscription {
            downloadStatus.ofIOToMainThread()
                    .subscribe{ downloadStatus ->
                        when (downloadStatus.status) {
                            DownloadStatus.DOWNLOADED -> view?.showMessage(R.string.podcast_already_downloaded)
                            DownloadStatus.DOWNLOADING -> view?.showMessage(R.string.podcast_download_started, app.getString(R.string.cancel)) {
                                storageInteractor.cancelDownload(downloadStatus)
                            }
                        }
                    }
        }
    }

    fun share(podcastItem: PodcastItem) {
        val url = BuildConfig.DETAIL_URL.format(podcastItem.remoteId.toString())
        val text = app.getString(R.string.share_text, podcastItem.userFriendlyTitle, url)

        ShareCompat.IntentBuilder.from(baseActivity)
                .setText(text)
                .setType("text/plain")
                .intent
                .startActivity(baseActivity)
    }

    fun openWith(podcastItem: PodcastItem) {
        Intent()
                .view(BuildConfig.DETAIL_URL.format(podcastItem.remoteId))
                .startActivity(baseActivity)
    }

    fun favorite(podcastItem: PodcastItem) {
        addSubscription {
            favoritedPodcastItemInteractor.addFavorite(podcastItem)
                    .ofIOToMainThread()
                    .subscribe {
                        view?.showMessage(R.string.podcast_favorited)
                    }
        }
    }

    fun removeFavorite(podcastItem: PodcastItem) {
        addSubscription {
            favoritedPodcastItemInteractor.removeFavorite(podcastItem)
                    .ofIOToMainThread()
                    .subscribe { removed ->
                        items.remove(podcastItem)

                        view?.showMessage(if (removed) R.string.podcast_removed_from_favorites else R.string.error_removing_favorites)
                        view?.remove(podcastItem)
                    }
        }
    }
}
