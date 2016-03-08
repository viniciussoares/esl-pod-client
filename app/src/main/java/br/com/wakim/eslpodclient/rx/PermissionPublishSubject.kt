package br.com.wakim.eslpodclient.rx

import android.content.pm.PackageManager
import rx.subjects.PublishSubject

class PermissionPublishSubject {

    companion object {
        val INSTANCE = PermissionPublishSubject(PublishSubject.create())
    }

    val publishSubject: PublishSubject<Permission>

    private constructor(publishSubject: PublishSubject<Permission>) {
        this.publishSubject = publishSubject
    }

    data class Permission(val requestCode: Int, val permissions: Array<out String>, val grantResults: IntArray) {
        fun isGranted() : Boolean {

            for (result: Int in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }

            return true
        }
    }
}