package kong.browser.components

import kong.browser.utils.*
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.NewElementAction
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnActionButton
import com.intellij.ui.components.JBLabel
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.tree.DefaultMutableTreeNode

// DialogWrapper
class BookmarkEntryDialogue(project: Project, title: String) : DialogWrapper(project, true) {
    private val bookmarkNameTF = JTextField()
    private val bookmarkURLTF = JTextField()

    constructor(project: Project, title: String, bookmark: Bookmark) : this(project, title) {
        bookmarkNameTF.text = bookmark.displayName
        bookmarkURLTF.text = bookmark.url
    }

    init {
        this.title = title
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val c = GridBagConstraints()

        panel.add(JBLabel("Name:"), c.weightedRowEntry(0, 0, 0.07))
        panel.add(bookmarkNameTF, c.weightedRowEntry(1, 0, 0.93))
        panel.add(JBLabel("Url:"), c.weightedRowEntry(0, 1, 0.07))
        panel.add(bookmarkURLTF, c.weightedRowEntry(1, 1, 0.93))

        panel.preferredSize = Dimension(300, 0)

        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent = bookmarkNameTF

    fun acquire(): Bookmark? {
        return if (showAndGet()) Bookmark(bookmarkNameTF.text!!, bookmarkURLTF.text!!) else null
    }
}


abstract class BrowserBookmarkAction() : AnActionButton(), DumbAware {
    open fun doAction(bookmarkManager: BookmarkManager, project: Project, nodes: Array<DefaultMutableTreeNode>) { }
    open fun doUpdateButton(e: AnActionEvent, node: DefaultMutableTreeNode) { }

    override fun actionPerformed(e: AnActionEvent) {
        e.withDataKey(BlastBrowser.DataKeys.TARGET_TREE) {
            it.getSelectedNodes(DefaultMutableTreeNode::class.java, null).whenNotEmpty {
                doAction(e.service<BookmarkManagerImpl>()!!, e.project!!, it)
            }
        }
    }

    final override fun updateButton(e: AnActionEvent) {
        val tree = e.data(BlastBrowser.DataKeys.TARGET_TREE)
        if (tree != null) tree.selectedNodes().whenSingleton{ doUpdateButton(e, it) }
        else e.presentation.isEnabled = false
    }
}

class AddNewBrowserBookmarkGroupAction() : NewElementAction() {
    override fun getGroup(dataContext: DataContext?): ActionGroup {
        return object : ActionGroup("New", true) {
            override fun getChildren(e: AnActionEvent?): Array<out AnAction> = arrayOf(
                    actionButton<AddBMAction>(icon = AllIcons.General.Web, text = "bookmark"),
                    actionButton<AddBMDAction>(icon = AllIcons.Nodes.Folder, text = "directory"))
        }
    }
}

class AddNewBrowserBookmarkGroupActionButton() : BrowserBookmarkAction() {
    override fun actionPerformed(e: AnActionEvent) {
        AddNewBrowserBookmarkGroupAction().actionPerformed(e)
    }

    override fun doUpdateButton(e: AnActionEvent, node: DefaultMutableTreeNode) {
        if (node.userObject is BookmarkDirectory) e.presentation.isEnabled = true
        else e.presentation.isEnabled = false
    }
}

class AddBMAction() : BrowserBookmarkAction() {
    override fun doAction(bookmarkManager: BookmarkManager, project: Project, nodes: Array<DefaultMutableTreeNode>) {
        val targetDirectory = nodes[0].userObject as BookmarkDirectory
        val addedBookmark = BookmarkEntryDialogue(project, "Add new bookmark in ${targetDirectory.displayName}").acquire()
        if (addedBookmark != null) bookmarkManager.addNode(targetDirectory, addedBookmark)
    }

    override fun doUpdateButton(e: AnActionEvent, node: DefaultMutableTreeNode) {
        if (node.userObject is BookmarkDirectory) e.presentation.isEnabled = true
        else e.presentation.isEnabled = false
    }
}

class AddBMDAction() : BrowserBookmarkAction() {
    override fun doAction(bookmarkManager: BookmarkManager, project: Project, nodes: Array<DefaultMutableTreeNode>) {
        val targetDirectory = nodes[0].userObject as BookmarkDirectory

        val name = Messages.showInputDialog(project,
                "enter name of new bookmark directory",
                "add new bookmark directory",
                null)

        if (!name.isNullOrEmpty()) {
            bookmarkManager.addNode(targetDirectory, BookmarkDirectory(name!!, xmlSafeUUID()))
        }
    }

    override fun doUpdateButton(e: AnActionEvent, node: DefaultMutableTreeNode) {
        if (node.userObject is BookmarkDirectory) e.presentation.isEnabled = true
        else e.presentation.isEnabled = false
    }
}


class EditBookmarkNodeEntryAction() : BrowserBookmarkAction() {
    override fun doAction(bookmarkManager: BookmarkManager, project: Project, nodes: Array<DefaultMutableTreeNode>) {
        val node = nodes[0].userObject as BookmarkNode
        when (node) {
            is BookmarkDirectory -> Messages.showInputDialog(project,
                    "update name of new bookmark directory",
                    "new bookmark directory name",
                    null, node.displayName, null).whenNotNull {
                node.displayName = it
                bookmarkManager.updateNode(node)
            }
            is BookmarkNode -> BookmarkEntryDialogue(project, "edit bookmark", node as Bookmark).acquire().whenNotNull {
                node.url = it.url
                node.displayName = it.displayName
                bookmarkManager.updateNode(node)
            }
        }
    }
}

class DeleteSelectedBookmarkAction() : BrowserBookmarkAction() {
    override fun doAction(bookmarkManager: BookmarkManager, project: Project, nodes: Array<DefaultMutableTreeNode>) {
        nodes.forEach {
            if(!it.isRoot) {
                val parentNode = (it.parent as DefaultMutableTreeNode)
                val parentDirectory = parentNode.userObject as BookmarkDirectory
                val itNode = it.userObject as BookmarkNode

                if (Messages.showYesNoDialog(project,
                        "Are you sure you want to delete ${itNode.type()} ${itNode.displayName}?",
                        "Delete Bookmark Directory",
                        null) == Messages.YES) {
                    bookmarkManager.removeNode(parentDirectory, it.userObject as BookmarkNode)
                }
            }
        }
    }
}