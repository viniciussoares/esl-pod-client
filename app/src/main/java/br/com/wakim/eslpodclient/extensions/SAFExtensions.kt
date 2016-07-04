package br.com.wakim.eslpodclient.extensions

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.support.annotation.RequiresApi
import java.io.File

fun Context?.isSAFEnabled() = Build.VERSION.SDK_INT > 19

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
fun Context.createUriFromSAFTree(path: String, mimeType: String, fileName: String): Uri {
    val treeUri = Uri.parse(path)
    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

    return DocumentsContract.createDocument(contentResolver, docUri, mimeType, fileName)
}

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
fun Context.deleteDocumentFromSAF(fileUri: Uri) {
    DocumentsContract.deleteDocument(contentResolver, fileUri)
}

fun Uri.toFile(): File {
    if (Build.VERSION.SDK_INT > 19 && isSAFUri()) {
        val id = DocumentsContract.getDocumentId(this)
        return getFileFromDocumentIdSAF(id)!!
    }

    return File(path)
}

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
fun String.getFolderFromSAFTree(): File? {
    val treeUri = Uri.parse(this)
    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

    return getFileFromDocumentIdSAF(DocumentsContract.getTreeDocumentId(docUri))
}

fun Uri.isSAFUri() =
        Build.VERSION.SDK_INT > 19 && "com.android.externalstorage.documents" == authority

private fun getFileFromDocumentIdSAF(id: String): File? {
    var file: File? = null

    val split = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    if (split.size >= 2) {
        val type = split[0]
        val path = split[1]

        val storagePoints = File("/storage").listFiles()

        if ("primary".equals(type, ignoreCase = true)) {
            val externalStorage = Environment.getExternalStorageDirectory()
            file = File(externalStorage, path)
        }

        var i = 0
        while (storagePoints != null && i < storagePoints.size && file == null) {
            val externalFile = File(storagePoints[i], path)

            if (externalFile.exists()) {
                file = externalFile
            }
            i++
        }
    }

    return file
}