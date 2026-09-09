package com.github.victorrentea.livecoding.drawio

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DrawioPngFileTypeTest : BasePlatformTestCase() {

    /**
     * The whole feature rests on this: `png` is already mapped to the platform's image type by
     * extension, and we claim the file with the wildcard `*.drawio.png`. Wildcard matchers are
     * consulted before extension ones, so ours wins — assert it rather than trust it.
     */
    fun testDrawioPngBeatsThePlainPngExtension() {
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName("ConceptualModel.drawio.png")
        assertSame(DrawioPngFileType, fileType)
    }

    fun testOrdinaryPngIsLeftToTheImageViewer() {
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName("screenshot.png")
        assertNotSame(DrawioPngFileType, fileType)
    }

    /** The source-only `.drawio` files are XML and stay editable in the IDE. */
    fun testPlainDrawioSourceIsNotClaimed() {
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName("ConceptualModel.drawio")
        assertNotSame(DrawioPngFileType, fileType)
    }
}
