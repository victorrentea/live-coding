package com.github.victorrentea.livecoding.autoreload

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import java.nio.file.Files
import java.nio.file.Path

class AutoReloadFromDiskServiceTest : LightJavaCodeInsightFixtureTestCase() {
    private lateinit var dir: Path
    private lateinit var file: VirtualFile

    override fun setUp() {
        super.setUp()
        dir = Files.createTempDirectory("auto-reload")
        val onDisk = Files.writeString(dir.resolve("Order.java"), "class Order {}")
        file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(onDisk)!!
        assertEquals("class Order {}", VfsUtilCore.loadText(file))
    }

    override fun tearDown() {
        try {
            AppSettingsState.getInstance().autoReloadFromDisk = true
            dir.toFile().deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testATickReloadsAFileChangedOnDisk() {
        Files.writeString(dir.resolve("Order.java"), "class Order { int total; }")

        service<AutoReloadFromDiskService>().tick()

        assertTrue(waitUntil { VfsUtilCore.loadText(file) == "class Order { int total; }" })
    }

    fun testATickSeesANewFile() {
        Files.writeString(dir.resolve("Customer.java"), "class Customer {}")

        service<AutoReloadFromDiskService>().tick()

        assertTrue(waitUntil { file.parent.findChild("Customer.java") != null })
    }

    fun testNothingIsReloadedWhenTheSettingIsOff() {
        AppSettingsState.getInstance().autoReloadFromDisk = false
        Files.writeString(dir.resolve("Order.java"), "class Order { int total; }")

        service<AutoReloadFromDiskService>().tick()

        assertFalse(waitUntil(1000) { VfsUtilCore.loadText(file) == "class Order { int total; }" })
    }

    private fun waitUntil(timeoutMillis: Long = 10_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            if (condition()) return true
            Thread.sleep(20)
        }
        return false
    }
}
