package br.com.wakim.eslpodclient.interactor

import br.com.wakim.eslpodclient.model.DownloadStatus
import rx.Single
import rx.SingleSubscriber
import java.io.File

class DownloadStatusOnSubscribe(private val remoteId: Long, private val localPath: String, private val downloadDbInteractor: DownloadDbInteractor): Single.OnSubscribe<DownloadStatus> {
    override fun call(subscriber: SingleSubscriber<in DownloadStatus>) {
        val download = downloadDbInteractor.getDownloadByRemoteId(remoteId)
        val file = File(localPath)

        if (subscriber.isUnsubscribed) {
            return
        }

        if (!file.exists()) {
            subscriber.onSuccess(DownloadStatus(localPath = localPath, remoteId = remoteId, downloadId = 0, status = DownloadStatus.NOT_DOWNLOADED))
        }

        // Not managed by app or database was cleared
        if (download == null) {
            subscriber.onSuccess(DownloadStatus(localPath = localPath, remoteId = remoteId, downloadId = 0, status = DownloadStatus.DOWNLOADED))
        } else {
            with (download) {
                subscriber.onSuccess(DownloadStatus(localPath = localPath, remoteId = remoteId, downloadId = downloadId, status = status))
            }
        }
    }
}