//package com.github.victorrentea.livecoding
//
//import com.intellij.openapi.components.*
//import com.intellij.openapi.options.Configurable
//import com.intellij.openapi.options.SearchableConfigurable
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.DialogWrapper
//import com.intellij.ui.AddDeleteListPanel
//import org.jdom.Element
//import java.awt.Component
//import java.util.*
//import javax.swing.JComponent
//import javax.swing.JPanel
//import javax.swing.JTextField
//
//
//class LiveCodingConfigurable : Configurable{
//    override fun createComponent(): JComponent? {
//        TODO("Not yet implemented")
//    }
//
//    override fun isModified(): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun apply() {
//        TODO("Not yet implemented")
//    }
//
//    override fun getDisplayName(): String = "Live-Coding"
//}
//
//@State(
//    name = "HaxeProjectSettings",
//    storages = [Storage(file = StoragePathMacros.PRODUCT_WORKSPACE_FILE)]
//)
//class HaxeProjectSettings : PersistentStateComponent<Element> {
//    private var userCompilerDefinitions = ""
////    val userCompilerDefinitionsAsSet =
////        get() = setOf(Arrays.asList(getUserCompilerDefinitions()))
//
//    fun getUserCompilerDefinitions(): List<String> {
//        return userCompilerDefinitions.split(",".toRegex()).toList()
//    }
//
//    fun setUserCompilerDefinitions(userCompilerDefinitions: List<String?>?) {
//        this.userCompilerDefinitions = userCompilerDefinitions
//            ?.filter { it != null && it.isNotEmpty() }
//            ?.joinToString(",")
//            ?: "";
//    }
//
//    override fun loadState(state: Element) {
//        userCompilerDefinitions = state.getAttributeValue(DEFINES, "")
//    }
//
//    override fun getState(): Element {
//        val element = Element(HAXE_SETTINGS)
//        element.setAttribute(DEFINES, userCompilerDefinitions)
//        return element
//    }
//
//    companion object {
//        const val HAXE_SETTINGS = "HaxeProjectSettings"
//        const val DEFINES = "defines"
//        fun getInstance(project: Project): HaxeProjectSettings {
//            return ServiceManager.getService(project, HaxeProjectSettings::class.java)
//        }
//    }
//}
//
//class HaxeSettingsConfigurable(project: Project) : SearchableConfigurable {
//    private var mySettingsPane: HaxeSettingsForm? = null
//    private val myProject: Project
//    override fun getDisplayName(): String {
//        return "Live-Coding"
//    }
//
//    override fun getId(): String {
//        return "live-coding"
//    }
//
//
//    override fun createComponent(): JComponent? {
//        if (mySettingsPane == null) {
//            mySettingsPane = HaxeSettingsForm()
//        }
//        reset()
//        return mySettingsPane!!.panel
//    }
//
//    override fun isModified(): Boolean {
//        return mySettingsPane != null && mySettingsPane!!.isModified(getSettings())
//    }
//
//    override fun apply() {
//        if (mySettingsPane != null) {
//            val modified = isModified
//            mySettingsPane!!.applyEditorTo(getSettings())
//            if (modified) {
//                println("Changed settings")
//            }
//        }
//    }
//
//    override fun reset() {
//        if (mySettingsPane != null) {
//            mySettingsPane!!.resetEditorFrom(getSettings())
//        }
//    }
//
//    private fun getSettings(): HaxeProjectSettings = HaxeProjectSettings.getInstance(myProject)
//
//    override fun disposeUIResources() {
//        mySettingsPane = null
//    }
//
//    override fun enableSearch(option: String): Runnable? {
//        return null
//    }
//
//    init {
//        myProject = project
//    }
//}
//
//class HaxeSettingsForm {
//    private val myPanel: JPanel? = null
//    private var myAddDeleteListPanel: MyAddDeleteListPanel? = null
//    val panel: JComponent?
//        get() = myPanel
//
//    fun isModified(settings: HaxeProjectSettings): Boolean {
//        val oldList: List<String> = settings.getUserCompilerDefinitions()
//        val newList: List<String> = myAddDeleteListPanel!!.items()
//        val isEqual = oldList.size == newList.size && oldList.containsAll(newList)
//        return !isEqual
//    }
//
//    fun applyEditorTo(settings: HaxeProjectSettings) {
//        settings.setUserCompilerDefinitions(myAddDeleteListPanel!!.items())
//    }
//
//    fun resetEditorFrom(settings: HaxeProjectSettings) {
//        myAddDeleteListPanel!!.removeALlItems()
//        for (item in settings.getUserCompilerDefinitions()) {
//            myAddDeleteListPanel!!.addItem(item)
//        }
//    }
//
//    private fun createUIComponents() {
//        myAddDeleteListPanel = MyAddDeleteListPanel("Panel Title")
//    }
//
//    private inner class MyAddDeleteListPanel(title: String?) :
//        AddDeleteListPanel<String?>(title, emptyList<String>()) {
//        fun addItem(item: String?) {
//            myListModel.addElement(item)
//        }
//
//        fun removeALlItems() {
//            myListModel.removeAllElements()
//        }
//
//        fun items() =  listItems.map { it.toString() }.toList()
//
//        override fun findItemToAdd(): String? {
//            val dialog = StringValueDialog(myAddDeleteListPanel!!, false)
//            dialog.show()
//            if (!dialog.isOK()) {
//                return null
//            }
//            val stringValue: String = dialog.getStringValue()
//            return if (stringValue != null && stringValue.isEmpty()) null else stringValue
//        }
//    }
//}
//
//class StringValueDialog(parent: Component, canBeParent: Boolean) :
//    DialogWrapper(parent, canBeParent) {
//    private val myTextField: JTextField? = null
//    private val myMainPanel: JPanel? = null
//    override fun createCenterPanel(): JComponent? {
//        return myMainPanel
//    }
//
//    fun getStringValue() = myTextField!!.text
//
//    override fun getPreferredFocusedComponent(): JComponent? {
//        return myTextField
//    }
//
//    init {
//        title = "Dialog title"
//        init()
//    }
//}