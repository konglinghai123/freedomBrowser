package kong.browser.utils

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.components.ServiceManager
import com.intellij.ui.AnActionButton
import com.intellij.ui.treeStructure.Tree
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode

inline fun <reified AB: AnActionButton> actionButton(
        icon: Icon? = null,
        shortcut: ShortcutSet? = null,
        text: String? = null,
        context: DataContext? = null,
        contextComponent: JComponent? = null
): AB {
    val res = AB::class.java.newInstance()
    icon.whenNotNull { res.templatePresentation.icon = it }
    shortcut.whenNotNull { res.shortcut = it }
    text.whenNotNull { res.templatePresentation.text = it }
    contextComponent.whenNotNull { res.contextComponent= it }

    return res
}

inline fun <reified T: Any> AnActionEvent.service(): T? {
    return ServiceManager.getService(this.project!!, T::class.java)
}


fun <T> AnActionEvent.data(dk: DataKey<T>): T? {
    return this.dataContext.getData(dk)
}

inline fun <T, R> AnActionEvent.withDataKey(dk: DataKey<T>, op: (T) -> R?): R? {
    val res = this.data(dk)
    return if(res != null) op(res) else null
}


fun Tree.selectedNodes(): Array<DefaultMutableTreeNode> {
    return this.getSelectedNodes(DefaultMutableTreeNode::class.java, null)
}

fun DefaultMutableTreeNode.parentUserObject(): Any = (this.parent!! as DefaultMutableTreeNode).userObject


