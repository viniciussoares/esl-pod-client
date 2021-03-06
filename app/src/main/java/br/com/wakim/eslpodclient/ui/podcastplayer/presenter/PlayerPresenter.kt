package br.com.wakim.eslpodclient.ui.podcastplayer.presenter

import br.com.wakim.eslpodclient.Application
import br.com.wakim.eslpodclient.R
import br.com.wakim.eslpodclient.android.receiver.ConnectivityException
import br.com.wakim.eslpodclient.android.service.PlayerCallback
import br.com.wakim.eslpodclient.android.service.PlayerService
import br.com.wakim.eslpodclient.android.service.StorageService
import br.com.wakim.eslpodclient.data.interactor.PodcastInteractor
import br.com.wakim.eslpodclient.data.model.DownloadStatus
import br.com.wakim.eslpodclient.data.model.PodcastItem
import br.com.wakim.eslpodclient.data.model.PodcastItemDetail
import br.com.wakim.eslpodclient.ui.podcastplayer.view.PlayerView
import br.com.wakim.eslpodclient.ui.presenter.Presenter
import br.com.wakim.eslpodclient.ui.rx.PermissionPublishSubject
import br.com.wakim.eslpodclient.ui.view.PermissionRequester
import br.com.wakim.eslpodclient.util.extensions.ofIOToMainThread
import rx.android.schedulers.AndroidSchedulers

class PlayerPresenter(private val app : Application,
                      private val permissionRequester: PermissionRequester,
                      private val podcastInteractor: PodcastInteractor) : Presenter<PlayerView>() {

    var podcastItem : PodcastItem? = null
    var podcastDetail : PodcastItemDetail? = null

    var playerService : PlayerService? = null
    var storageService : StorageService? = null

    var playPending : Boolean = false

    val playerCallback = object : PlayerCallback {

        override fun onAudioFocusFailed() { }

        override fun onDurationChanged(duration: Int) {
            view?.setMaxProgress(if (duration < 0) 0 else duration)
        }

        override fun onCacheProgress(position: Int) {
            view?.setSecondaryProgressValue(position)
        }

        override fun onSeekAvailable(available: Boolean) {
            view?.setSeekEnabled(available)
        }

        override fun onPositionChanged(position: Int) {
            view?.setProgressValue(position)
        }

        override fun onPlayerPreparing() {
            view?.showLoadingButton()
        }

        override fun onPlayerStarted() {
            view?.showPauseButton()
        }

        override fun onPlayerPaused() {
            view?.showPlayButton()
        }

        override fun onPlayerStopped() {
            view?.let {
                it.setProgressValue(0)
                it.showPlayButton()
            }
        }

        override fun onStreamTypeResolved(streamType: Long) {
            view?.setStreamType(streamType)
        }

        override fun onSkippedToPrevious(podcastItem: PodcastItem) {
            view?.bindPodcastItem(podcastItem)
            loadDetail(podcastItem)
        }

        override fun onSkippedToNext(podcastItem: PodcastItem) {
            view?.bindPodcastItem(podcastItem)
            loadDetail(podcastItem)
        }

        override fun onConnectivityError() {
            view?.showMessage(R.string.no_connectivity)
        }
    }

    fun onRestoreInstanceState(podcastItem: PodcastItem?, podcastDetail: PodcastItemDetail?) {
        this.podcastItem = podcastItem
        this.podcastDetail = podcastDetail
    }

    override fun onStart() {
        super.onStart()

        addSubscription {
            PermissionPublishSubject
                    .INSTANCE
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe { permission ->
                        with (permission) {
                            if (requestCode == Application.LIST_DOWNLOAD_WRITE_STORAGE_PERMISSION) {
                                if (isGranted()) {
                                    startDownload()
                                } else {
                                    view!!.showMessage(R.string.write_external_storage_permission_needed_to_download)
                                }
                            } else if (requestCode == Application.PLAYER_WRITE_STORAGE_PERMISSION) {
                                if (isGranted()) {
                                    doPlay()
                                } else {
                                    view!!.showMessage(R.string.write_external_storage_permission_needed_to_cache_streaming)
                                }
                            }

                            permission
                        }
                    }
        }
    }

    override fun onStop() {
        super.onStop()
        unbindToService()
    }

    fun unbindToService() {
        playerService?.let {
            it.callback = null
        }

        playerService = null
        storageService = null
    }

    fun setServices(playerService: PlayerService, storageService: StorageService) {
        this.playerService = playerService
        this.storageService = storageService

        playerService.callback = playerCallback

        setupInitialState()
        doPlay()
    }

    fun setupInitialState() {
        playerService?.apply {
            if (isPlaying()) {
                view!!.showPauseButton()
            }

            getPodcastItem()?.let {
                podcastItem = it

                view?.let {
                    it.bindPodcastItem(podcastItem!!)
                    it.setVisible()

                    if (!isPlaying()) {
                        it.showPlayButton()
                    } else {
                        it.setMaxProgress(getDuration().toInt())
                    }

                    it.setStreamType(getStreamType())
                }

                loadDetail(it)
            }
        }
    }

    fun explicitlyStop() {
        playerService?.dispose()
    }

    fun onPauseClicked() {
        playerService?.pause()
    }

    fun onPlayClicked() {
        playPending = true
        doPlay()
    }

    fun onStopClicked() {
        playerService?.stop()
    }

    fun onNextClicked() {
        playerService?.skipToNext()
    }

    fun onPreviousClicked() {
        playerService?.skipToPrevious()
    }

    private fun doPlay() {
        if (!playPending) return

        if (playerService == null || storageService == null) {
            return
        }

        if (storageService!!.shouldCache() && !permissionRequester.hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissionRequester.requestPermissions(Application.PLAYER_WRITE_STORAGE_PERMISSION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        val item = podcastItem as PodcastItem

        playerService?.let {
            it.reset()
            it.play(item, view?.getProgressValue() ?: 0)

            playPending = false
        }
    }

    fun startDownload() {
        podcastItem?.let {
            if (!permissionRequester.hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionRequester.requestPermissions(Application.LIST_DOWNLOAD_WRITE_STORAGE_PERMISSION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

                return
            }

            storageService!!.startDownloadIfNeeded(it)
                .subscribe { downloadStatus ->
                    when (downloadStatus.status) {
                        DownloadStatus.DOWNLOADED -> view?.showMessage(R.string.podcast_already_downloaded)
                        DownloadStatus.DOWNLOADING -> view?.showMessage(R.string.podcast_download_started, app.getString(R.string.cancel)) {
                            storageService?.cancelDownload(downloadStatus)
                        }
                    }
                }
        }
    }

    fun seekTo(pos : Int) {
        playerService?.seek(pos)
    }

    fun play(podcastItem: PodcastItem) {
        this.podcastItem = podcastItem

        view!!.setProgressValue(0)

        onPlayClicked()
        loadDetail(podcastItem)
    }

    fun isPlaying() = playerService?.isPlaying() ?: false

    // Details

    fun loadDetail(podcastItem: PodcastItem) {
        view!!.setLoading(true)

        if (podcastDetail?.remoteId == podcastItem.remoteId) {
            bindDetail()
            return
        }

        addSubscription() {
            podcastInteractor.getPodcastDetail(podcastItem)
                    .ofIOToMainThread()
                    .subscribe (
                            { detail ->
                                podcastDetail = detail
                                bindDetail()
                            },
                            { e: Throwable ->
                                if (e is ConnectivityException)
                                    view?.showMessage(R.string.no_connectivity)
                            }
                    )
        }
    }

    fun bindDetail() {
        view?.let {
            it.setLoading(false)
            it.bindPodcastDetail(podcastDetail!!)
        }
    }

    fun seekToSlowDialog() {
        podcastDetail?.seekPos?.let {
            playerService?.seek(it.slow * 1000)
        }
    }

    fun seekToExplanation() {
        podcastDetail?.seekPos?.let {
            playerService?.seek(it.explanation * 1000)
        }
    }

    fun seekToNormalDialog() {
        podcastDetail?.seekPos?.let {
            playerService?.seek(it.normal * 1000)
        }
    }
}