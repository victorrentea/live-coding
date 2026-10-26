package com.github.victorrentea.livecoding.tokens

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class TokenCountWidgetTest : LightJavaCodeInsightFixtureTestCase() {

    fun testWidgetShowsClawdAndTogglesHighlightingOnClick() {
        val settings = AppSettingsState.getInstance()
        val before = settings.highlightTokens
        val widget = TokenCountWidget(project)
        try {
            assertEquals(TokenCountWidget.ID, widget.ID())
            assertSame(LiveCodingIcons.Clawd, (widget.component as com.intellij.openapi.wm.impl.status.TextPanel.WithIconAndArrows).icon)
            assertNotNull(widget.text)
            assertFalse("no '(GPT)' suffix any more - the model lives in the tooltip", widget.text.contains("tok"))

            TokenCountService.getInstance(project).highlightTokens = !before
            assertEquals(!before, settings.highlightTokens)
        } finally {
            settings.highlightTokens = before
            Disposer.dispose(widget)
        }
    }
}
