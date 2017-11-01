package kong.browser.components

import kong.browser.utils.inSwingThread
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.icons.AllIcons
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.teamdev.jxbrowser.chromium.Browser
import com.teamdev.jxbrowser.chromium.BrowserPreferences
import com.teamdev.jxbrowser.chromium.BrowserType
import com.teamdev.jxbrowser.chromium.LoggerProvider
import com.teamdev.jxbrowser.chromium.events.*
import com.teamdev.jxbrowser.chromium.swing.BrowserView
import org.jdom.Element
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.beans.PropertyChangeListener
import java.util.logging.Level
import javax.swing.*

abstract class BaseBrowserEditor : UserDataHolderBase(), FileEditor, DataProvider {
    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null

    override fun isValid(): Boolean {
        return true
    }

    override fun getCurrentLocation(): FileEditorLocation {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun isModified(): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addPropertyChangeListener(p0: PropertyChangeListener) {
    }

    override fun removePropertyChangeListener(p0: PropertyChangeListener) {
    }
}

class BrowserFileIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        if ((file as? URLVirtualFileNode) != null) return AllIcons.General.Web
        else return null
    }
}

class JxBrowserEditor(val project: Project, val urlNode: URLVirtualFileNode) : BaseBrowserEditor() {
    private val root = JPanel(GridBagLayout(), true)
    private val textField = JBTextField()
    private val backButton = JButton("<-")
    private val forwardButton = JButton("->")
    private val browser: Browser
    private val browserView: BrowserView
    private val fileManagerEx = FileEditorManagerEx.getInstanceEx(project)

    init {
        browser = JxBrowserManager.initializeJxBrowser(BrowserType.HEAVYWEIGHT)
        browserView = BrowserView(browser)
        createComponent()

        textField.text = urlNode.targetUrl.toString()

        textField.addActionListener { e -> browser.loadURL(e.actionCommand.toString()) }
        backButton.addActionListener { if (browser.canGoBack()) browser.goBack() }
        forwardButton.addActionListener { if (browser.canGoForward()) browser.goForward() }

        browser.addTitleListener {
            if (!urlNode.name.equals(it.title)) {
                urlNode.name = it.title
                fileManagerEx.updateFilePresentation(urlNode)

            }
        }


        browser.addLoadListener(object : LoadAdapter() {
            override fun onDocumentLoadedInMainFrame(loadEvent: LoadEvent) {
                // update the VirtualFile so that splits from this editor point to the same browser.
                urlNode.targetUrl = loadEvent.browser.url!!

                loadEvent.inSwingThread {
                    textField.text = it.browser.url
                }
            }
        })
    }

    override fun deselectNotify() {
    }

    override fun selectNotify() {
        if (FileEditorManagerEx.getInstanceEx(project).selectedEditors.find { it == this } != null) {
            if (!browser.isLoading && !browser.url.equals(urlNode.targetUrl)) browser.loadURL(urlNode.targetUrl)
        }
    }

    override fun dispose() = browser.dispose()

    override fun setState(state: FileEditorState) {
        val st = (state as BrowserEditorState)
        SwingUtilities.invokeLater {
            urlNode.name = st.title
            fileManagerEx.updateFilePresentation(urlNode)
        }
    }

    // when browsers are being saved
    override fun getState(level: FileEditorStateLevel): FileEditorState {
        // TODO: dont save if the browser has some sort of error
        return BrowserEditorState(urlNode.name, urlNode.url)
    }

    override fun getData(dataId: String): Any? {
        when (dataId) {
            BlastBrowser.DataKeys.TARGET_TREE.name -> return this
        }
        return null
    }

    private fun createComponent() {
        val c = GridBagConstraints()
        c.fill = GridBagConstraints.HORIZONTAL
        c.weightx = 1.0
        c.gridx = 0
        c.gridy = 0
        root.add(textField, c)

        c.fill = GridBagConstraints.NONE
        c.weightx = 0.0
        c.gridx = 1
        c.gridy = 0
        root.add(backButton, c)

        c.fill = GridBagConstraints.NONE
        c.weightx = 0.0
        c.gridx = 2
        c.gridy = 0
        root.add(forwardButton, c)

        c.fill = GridBagConstraints.BOTH
        c.gridx = 0
        c.gridy = c.gridy + 1
        c.weightx = 1.0
        c.weighty = 1.0
        c.gridwidth = 3
        c.ipady
        root.add(browserView, c)
    }

    override fun getComponent(): JComponent = root
    override fun getPreferredFocusedComponent(): JComponent = browserView
}

class BrowserEditorState(val title: String, val url: String) : FileEditorState {
    override fun canBeMergedWith(otherState: FileEditorState?, level: FileEditorStateLevel?): Boolean {
        return false
    }
}

class BrowserEditorProvider : FileEditorProvider, DumbAware {
    init {
        // todo move to a component initializer : not so bad since it is a singleton
        LoggerProvider.getBrowserLogger().level = Level.WARNING
        LoggerProvider.getIPCLogger().level = Level.SEVERE
        LoggerProvider.getChromiumProcessLogger().level = Level.SEVERE
    }

    override fun getEditorTypeId(): String = "blast.browser.editor"

    override fun createEditor(project: Project, vf: VirtualFile): FileEditor {
        JxBrowserManager.ensurePlatformJarDownloaded(project, null)
        return JxBrowserEditor(project, (vf as URLVirtualFileNode))
    }

    override fun accept(project: Project, file: VirtualFile): Boolean = file is URLVirtualFileNode

    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState {
        return BrowserEditorState(
                sourceElement.getAttributeValue("title"),
                sourceElement.getAttributeValue("url"))
    }

    override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
        val st = (state as BrowserEditorState)
        targetElement.setAttribute("title", st.title)
        targetElement.setAttribute("url", st.url)
    }
}