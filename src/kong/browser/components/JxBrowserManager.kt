package kong.browser.components

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.download.DownloadableFileDescription
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.download.DownloadableFileSetDescription
import com.teamdev.jxbrowser.chromium.*
import com.teamdev.jxbrowser.chromium.internal.*
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent

object JxBrowserManager {
    private val logger = Logger.getInstance("#my.logger")

    private val JX_BROWSER_VERSION = "6.14"

    private val JX_BROWSER_REPO = "http://maven.teamdev.com/repository/products"
    private val JX_BROWSER_ARTIFACT_BASE_PATH = "/com/teamdev/jxbrowser"

    private val JX_BROWSER_ARTIFACT = browserArtifact("jxbrowser")
    private val JX_BROWSER_LICENSE_ARTIFACT = "license.jar"
    private val JX_BROWSER_WIN_ARTIFACT = "jxbrowser-win"
    private val JX_BROWSER_MAC_ARTIFACT = "jxbrowser-mac"
    private val JX_BROWSER_LINUX_ARTIFACT_32 = "jxbrowser-linux32"
    private val JX_BROWSER_LINUX_ARTIFACT_64 = "jxbrowser-linux64"


    private val artifactNameStem = makeArtifactStem()
    private val libDir: Path = Paths.get((PathManager.getPluginsPath() + "${File.separatorChar}blast.browser" + "${File.separatorChar}lib"))!!
    private val artifactFilename = "$artifactNameStem-$JX_BROWSER_VERSION.jar"
    private val artifactUrl = "$JX_BROWSER_REPO$JX_BROWSER_ARTIFACT_BASE_PATH/$artifactNameStem/$JX_BROWSER_VERSION/$artifactFilename"
    private val targetFile = "${libDir.resolve(artifactFilename).toString()}"

    fun browserArtifact(name: String): String = "$name-$JX_BROWSER_VERSION.jar"

    fun pluginEmbeddedOrSandboxJarFileUrl(name: String): String {
        val asBundledPlugin = Thread.currentThread().contextClassLoader.getResource("/$name")?.toString()
        if (asBundledPlugin == null) {
            val plc = Browser::class.java.classLoader as PluginClassLoader
            return plc.urls.find { it.toString().endsWith(name) }!!.toString()
        } else return asBundledPlugin
    }

    init {
        libDir.toFile().mkdirs()
    }

    fun ensurePlatformJarDownloaded(project: Project, parent: JComponent?): Boolean {
        if(!File(targetFile).exists()) {
            val res = Messages.showOkCancelDialog(
                    project,
                    "blast.browser needs the platform artefact for JxBrowser: $artifactFilename. Proceed with Download ?",
                    "JxBrowser artifact missing", "Download", "Later", null)
            if(res == Messages.CANCEL) return false

            val createFileDescription = DownloadableFileService.getInstance().createFileDescription(artifactUrl, artifactFilename)
            val downloader = DownloadableFileService.getInstance().createDownloader(object : DownloadableFileSetDescription {
                override fun getFiles(): MutableList<out DownloadableFileDescription> = mutableListOf(createFileDescription)
                override fun getVersionString(): String = JX_BROWSER_VERSION
                override fun getName(): String = artifactFilename
            })
            downloader.downloadFilesWithProgress(libDir.toString(), project, parent) ?: return false
        }
        return true
    }

    fun initializeJxBrowser(type: BrowserType): Browser {
        val platformArtefact = "file:$targetFile"
        BrowserPreferences.setChromiumDir(libDir.toString());

        logger.debug("initializing JxBrowser")

        val jxBrowserArtifactUrl = pluginEmbeddedOrSandboxJarFileUrl(JX_BROWSER_ARTIFACT)

        val cl = URLClassLoader(arrayOf(
                URL(platformArtefact),
                URL(jxBrowserArtifactUrl)
        ))

        cl.loadClass("com.teamdev.jxbrowser.chromium.BrowserContext")
                    .getMethod("defaultContext")
                    .invoke(null)


        return Browser(type)
    }

    private fun candidateArtifacts(): Array<File> =
            libDir.toFile().listFiles().filter { it.name.startsWith("$artifactNameStem-") }.toTypedArray()

    private fun versionNumber(artifactFile: File): String {
        return artifactFile.name.replace("$artifactNameStem-", "").replace(".jar", "")
    }

    private fun makeArtifactStem(): String {
        if (SystemInfo.isMac) return JX_BROWSER_MAC_ARTIFACT
        else if (SystemInfo.isWindows) return JX_BROWSER_WIN_ARTIFACT
        else if (SystemInfo.isLinux) {
            if (SystemInfo.is64Bit) return JX_BROWSER_LINUX_ARTIFACT_64
            else return JX_BROWSER_LINUX_ARTIFACT_32
        } else throw Exception("unrecognized os")
    }

}


