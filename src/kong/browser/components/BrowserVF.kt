package kong.browser.components

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem

class BrowserStorageVirtualFilesystem : VirtualFileSystem() {
    companion object {
        val protocol = "browserfs"
        val instance = VirtualFileManager.getInstance().getFileSystem(protocol) as BrowserStorageVirtualFilesystem
    }

    override fun getProtocol(): String = BrowserStorageVirtualFilesystem.protocol

    override fun findFileByPath(path: String): VirtualFile = URLVirtualFileNode("", path, this)

    override fun refreshAndFindFileByPath(p0: String): VirtualFile = findFileByPath(p0)

    override fun renameFile(p0: Any?, p1: VirtualFile, p2: String) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createChildFile(p0: Any?, p1: VirtualFile, p2: String): VirtualFile {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copyFile(p0: Any?, p1: VirtualFile, p2: VirtualFile, p3: String): VirtualFile {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun refresh(p0: Boolean) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteFile(p0: Any?, p1: VirtualFile) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createChildDirectory(p0: Any?, p1: VirtualFile, p2: String): VirtualFile {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addVirtualFileListener(p0: VirtualFileListener) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isReadOnly(): Boolean = true

    override fun removeVirtualFileListener(p0: VirtualFileListener) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun moveFile(p0: Any?, p1: VirtualFile, p2: VirtualFile) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

abstract class NonFileVirtualFile(protected val filesystem_: VirtualFileSystem) : VirtualFile() {
    companion object {
        protected val emptyByteArray_ = ByteArray(0)
    }

    override fun contentsToByteArray() = emptyByteArray_
    override fun getLength(): Long = 0

    override fun getInputStream() = throw UnsupportedOperationException()
    override fun getOutputStream(p0: Any?, p1: Long, p2: Long) = throw UnsupportedOperationException()

    override fun isWritable(): Boolean = true
    override fun isValid(): Boolean = true

    override fun refresh(p0: Boolean, p1: Boolean, p2: Runnable?) { }
    override fun getTimeStamp(): Long = 0
    override fun getModificationStamp(): Long =0

    override fun getFileSystem(): VirtualFileSystem = filesystem_
}

class URLVirtualFileNode(friendlyName: String, var targetUrl: String, filesystem_: BrowserStorageVirtualFilesystem): NonFileVirtualFile(filesystem_) {
    private var name_: String = friendlyName

    fun setName(name_: String ){
        this.name_ = name_
    }

    override fun getPath(): String {
        return targetUrl.toString()
    }

    override fun isDirectory(): Boolean {
        return false
    }


    override fun getName(): String {
        return name_
    }

    override fun getParent(): VirtualFile? = null

    override fun getChildren(): Array<out VirtualFile> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}