package com.github.victorrentea.livecoding.tokens

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class TokenCountWidgetTest : LightJavaCodeInsightFixtureTestCase() {

    fun testWidgetBuildsItsModelPopup() {
        val widget = TokenCountWidget(project)
        try {
            assertEquals(TokenCountWidget.ID, widget.ID())
            assertNotNull(widget.getSelectedValue())
            assertNotNull(widget.getTooltipText())
            // the click handler must not blow up on a project with no status bar attached
            val popup = widget.getPopup()
            assertNotNull(popup)
            popup.cancel()
        } finally {
            Disposer.dispose(widget)
        }
    }
}
