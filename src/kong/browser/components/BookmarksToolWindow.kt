package kong.browser.components

import kong.browser.utils.actionButton
import com.intellij.ide.dnd.DnDDragStartBean
import com.intellij.ide.dnd.DnDEvent
import com.intellij.ide.dnd.DnDSupport
import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.pom.Navigatable
import com.intellij.ui.CommonActionsPanel
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.awt.RelativeRectangle
import com.intellij.ui.content.ContentFactory
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.ToolTipManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class BookmarkTreeBuilder(jtree: JTree,
                          treeModel: DefaultTreeModel,
                          treeBase: AbstractTreeStructure) : AbstractTreeBuilder(jtree, treeModel, treeBase, null)

class BrowserOpener(private val friendlyName: String, private val url: String, private val project: Project) : Navigatable {
    override fun navigate(requestFocus: Boolean) {
        val manager = FileEditorManager.getInstance(project)
        val vf: URLVirtualFileNode = URLVirtualFileNode(friendlyName, url, BrowserStorageVirtualFilesystem.instance)
        manager.openFile(vf, true)
    }

    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true
}

// removed interface DockContainer
class BookmarkTreeViewPanel(
        val project: Project,
        val bookmarkManager: BookmarkManagerImpl,
        toolWindowEx: ToolWindowEx
) : JPanel(BorderLayout()), DataProvider, DataContext {
    private val root = DefaultMutableTreeNode()
    private val treeModel: DefaultTreeModel

    val tree: DnDAwareTree
    val treeBuilder: BookmarkTreeBuilder


    init {
        root.userObject = bookmarkManager.rootElement
        treeModel = DefaultTreeModel(root)
        tree = DnDAwareTree(treeModel)

        treeBuilder = BookmarkTreeBuilder(tree, treeModel, bookmarkManager)
//        DockManager.getInstance(project).register(this)

        TreeUtil.installActions(tree)
        UIUtil.setLineStyleAngled(tree)
        tree.isRootVisible = true
        tree.showsRootHandles = true
        tree.isLargeModel = true

        TreeSpeedSearch(tree)

        ToolTipManager.sharedInstance().registerComponent(tree)

        tree.cellRenderer = object : NodeRenderer() {}


        EditSourceOnDoubleClickHandler.install(tree)
        // use the Runnable variant below this will request focus.
        EditSourceOnEnterKeyHandler.install(tree) {}



        val decorator = ToolbarDecorator
                .createDecorator(tree)
                .initPosition()
                .disableAddAction()
                .disableRemoveAction()
                .disableDownAction()
                .disableUpAction()
                .addExtraActions(
                        actionButton<AddNewBrowserBookmarkGroupActionButton>(
                                icon = CommonActionsPanel.Buttons.ADD.icon,
                                shortcut = CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD),
                                contextComponent = this),
                        actionButton<EditBookmarkNodeEntryAction>(
                                icon = CommonActionsPanel.Buttons.EDIT.icon,
                                shortcut = CommonShortcuts.CTRL_ENTER,
                                contextComponent = this),
                        actionButton<DeleteSelectedBookmarkAction>(
                                icon = CommonActionsPanel.Buttons.REMOVE.icon,
                                shortcut = CustomShortcutSet.fromString("DELETE", "BACK_SPACE"),
                                contextComponent = this)
                )


        val action = ActionManager.getInstance().getAction(IdeActions.ACTION_NEW_ELEMENT)
        action.registerCustomShortcutSet(action.shortcutSet, tree)
        val panel = decorator.createPanel()

        panel.border = IdeBorderFactory.createEmptyBorder()
        add(panel, BorderLayout.CENTER)
        border = IdeBorderFactory.createEmptyBorder()



//        val additionalToolwindowGearActions = DefaultActionGroup()
//        additionalToolwindowGearActions.add(object: AnAction("show roots") {
//            override fun actionPerformed(e: AnActionEvent?) {
//
//            }
//        })

        toolWindowEx.setAdditionalGearActions(object:DefaultActionGroup() {

        })

        bookmarkManager.addBookmarkListener(object : BookmarkListener {
            override fun itemUpdated(node: BookmarkNode) {
                treeBuilder.queueUpdateFrom(node, false)
                tree.repaint()
            }

            override fun parentChanged(parent: BookmarkDirectory) {
                treeBuilder.queueUpdateFrom(parent, false)
                tree.repaint()
            }

            override fun rootsChanged() {
                treeBuilder.queueUpdateFrom(bookmarkManager.root, false)
                tree.repaint()
            }

            override fun itemAdded(parent: BookmarkDirectory, node: BookmarkNode) {
            }

            override fun itemRemoved(parent: BookmarkDirectory, node: BookmarkNode) {
                treeBuilder.select(parent)
            }
        })

        setupDND(tree)
    }

    fun setupDND(tree: DnDAwareTree) {
        DnDSupport.createBuilder(tree)
                .setBeanProvider {
                    val path = tree.getPathForLocation(it.point.x, it.point.y)
                    if (path != null) {
                        DnDDragStartBean(path)
                    } else {
                        object : DnDDragStartBean("") {
                            override fun isEmpty(): Boolean = true
                        }
                    }
                }
                .setTargetChecker {
                    val path = tree.getPathForLocation(it.point.x, it.point.y)

                    if (path == null) {
                        it.isDropPossible = false
                        false
                    } else if (tree.selectionCount > 1) {
                        it.isDropPossible = false
                        false
                    } else {
                        val targetNode = (path.lastPathComponent as DefaultMutableTreeNode).userObject as BookmarkNode
                        val bounds = tree.getPathBounds(path)

                        val sourceObject = (((it.attachedObject as TreePath).lastPathComponent) as DefaultMutableTreeNode).userObject as BookmarkNode

                        if (sourceObject == targetNode) {
                            it.isDropPossible = false
                            false
                        } else {
                            it.isDropPossible = true
                            if (bounds != null) {
                                if (targetNode is BookmarkDirectory)
                                    it.setHighlighting(RelativeRectangle(tree, bounds), DnDEvent.DropTargetHighlightingType.RECTANGLE)
                                else {
                                    val below = it.point.y >= bounds.y + bounds.height / 2
                                    if (below) {
                                        if (sourceObject.parent == targetNode.parent && sourceObject.index == targetNode.index + 1) it.isDropPossible = false
                                        println("below")
                                        bounds.y += bounds.height - 2
                                    } else {
                                        if (sourceObject.parent == targetNode.parent && sourceObject.index == targetNode.index - 1)
                                            it.isDropPossible = false
                                    }

                                    bounds.height = 2
                                    it.setHighlighting(RelativeRectangle(tree, bounds), DnDEvent.DropTargetHighlightingType.RECTANGLE)
                                }
                            }
                            false
                        }
                    }
                }
                .setDropHandler {
                    val path = tree.getPathForLocation(it.point.x, it.point.y)
                    val sourceObject = (((it.attachedObject as TreePath).lastPathComponent) as DefaultMutableTreeNode).userObject as BookmarkNode
                    val targetObject = (path.lastPathComponent as DefaultMutableTreeNode).userObject as BookmarkNode

                    if (targetObject is BookmarkDirectory) {
                        bookmarkManager.removeNode((sourceObject.parent as BookmarkDirectory), sourceObject)
                        bookmarkManager.addNode(targetObject, sourceObject)
                    } else if (targetObject is Bookmark) {
                        val sourceParent = (sourceObject.parent as BookmarkDirectory)
                        val targetParent = (targetObject.parent as BookmarkDirectory)
                        val targetObjectIndex = targetObject.index

                        val areSiblings = sourceParent == targetParent

                        bookmarkManager.removeNode(sourceParent, sourceObject)
                        val b = tree.getPathBounds(path)
                        val below = it.point.y >= b.y + b.height / 2



                        if (below) // below means higher index !
                            bookmarkManager.addNodeAt(targetParent, sourceObject, targetObjectIndex)
                        else {
                            if (areSiblings && sourceObject.index < targetObject.index)
                                bookmarkManager.addNodeAt(targetParent, sourceObject, targetObjectIndex - 1)
                        }
                    }
                }
                .install()
    }

    override fun getData(dataId: String): Any? {
        when (dataId) {
            CommonDataKeys.NAVIGATABLE.name -> {
                val res = tree.selectionPaths
                if (res.size == 1) {
                    val tp: TreePath = res[0]
                    val uo = (tp.lastPathComponent as DefaultMutableTreeNode).userObject
                    when (uo) { is Bookmark -> return BrowserOpener(uo.displayName, uo.url, project)
                    }
                }
            }
            BlastBrowser.DataKeys.TARGET_TREE.name -> return tree

            else -> return null
        }
        return null
    }
}

class BookmarkTreeViewToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val bookmarkManager = ServiceManager.getService(project, BookmarkManagerImpl::class.java)
        val panel = BookmarkTreeViewPanel(project, bookmarkManager, toolWindow as ToolWindowEx)
        val content = ContentFactory.SERVICE.getInstance().createContent(panel, "browser", false)
        toolWindow.contentManager.addContent(content)
    }
}