package br.com.wakim.eslpodclient.rx

import rx.subjects.PublishSubject

class DownloadPublishSubject {

    companion object {
        val INSTANCE = PublishSubject.create<Long>()
    }
}