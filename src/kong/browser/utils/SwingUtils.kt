package kong.browser.utils

import com.teamdev.jxbrowser.chromium.events.BrowserEvent
import java.awt.GridBagConstraints
import javax.swing.SwingUtilities

fun <T: BrowserEvent> T.inSwingThread(opt : (T) -> Unit) = SwingUtilities.invokeLater { opt(this) }

fun GridBagConstraints.weightedRowEntry(x: Int, y: Int, w: Double): GridBagConstraints {
    this.fill = GridBagConstraints.HORIZONTAL
    this.gridx = x
    this.gridy = y
    this.weightx = w
    return this
}