package io.papermc.unpick

import cuchaz.enigma.api.service.GuiService
import cuchaz.enigma.api.service.GuiService.MenuRegistrar
import cuchaz.enigma.api.view.GuiView
import cuchaz.enigma.api.view.ProjectView
import cuchaz.enigma.api.view.entry.ClassEntryView
import cuchaz.enigma.api.view.entry.FieldEntryView
import cuchaz.enigma.api.view.entry.LocalVariableEntryView
import cuchaz.enigma.api.view.entry.MethodEntryView
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.RecordComponentNode
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.lang.constant.ConstantDescs
import javax.swing.KeyStroke

class UnpickGuiService : GuiService {

    override fun addToEditorContextMenu(gui: GuiView, registrar: MenuRegistrar) {
        registrar.addSeparator()

        registrar.add(UnpickI18nService.COPY_TARGET_REFERENCE)
            .setEnabledWhen { referenceOnCursor(gui) != null }
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK)) // hopefully keybinds will be controllable at some point
            .setAction {
                val textToCopy = referenceOnCursor(gui) ?: return@setAction
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(textToCopy), null)
            }
        registrar.add(UnpickI18nService.COPY_CONSTANT_REFERENCE)
            .setEnabledWhen { eligibleConstantOnCursor(gui) != null }
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK))
            .setAction {
                val field = eligibleConstantOnCursor(gui) ?: return@setAction
                val textToCopy = field.parent.jvmClassName() + "." + field.name
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(textToCopy), null)
            }
    }

    private fun referenceOnCursor(gui: GuiView): String? {
        val project = gui.project ?: return null
        val hoveredReference = gui.cursorReference ?: return null

        return when (val entry = hoveredReference.entry) {
            is ClassEntryView if (project.jarIndex.entryIndex.getAccess(entry) and Opcodes.ACC_ANNOTATION) != 0 -> {
                "target_annotation ${entry.jvmClassName()}"
            }

            is ClassEntryView if (project.jarIndex.entryIndex.getAccess(entry) and Opcodes.ACC_RECORD) != 0 -> {
                val components = project.getBytecode(entry.fullName)?.recordComponents ?: return null
                "target_method ${entry.jvmClassName()} ${ConstantDescs.INIT_NAME} ${buildCanonicalConstructorDescriptor(components)}"
            }

            // if the cursor is not on the field declaration then fallback to target_field
            is FieldEntryView if gui.cursorDeclaration != null && (project.jarIndex.entryIndex.getAccess(entry.parent) and Opcodes.ACC_RECORD) != 0 -> {
                val components = project.getBytecode(entry.parent.fullName)?.recordComponents ?: return null
                val paramIndex = paramIndex(components, entry.name) // -1 for other fields
                if (paramIndex == -1) entry.toTargetReference() else "param $paramIndex"
            }

            is FieldEntryView -> entry.toTargetReference()
            is MethodEntryView -> entry.toTargetReference()

            is LocalVariableEntryView if entry.isArgument -> {
                val paramIndex = paramIndex(project, entry) // should never be -1 except for https://github.com/FabricMC/Enigma/issues/572
                if (paramIndex == -1) null else "param $paramIndex"
            }

            else -> null
        }
    }

    private fun paramIndex(project: ProjectView, local: LocalVariableEntryView): Int {
        val paramTypes = Type.getArgumentTypes(local.parent.descriptor)
        var lvtIndex = if ((project.jarIndex.entryIndex.getAccess(local.parent) and Opcodes.ACC_STATIC) != 0) 0 else 1

        paramTypes.forEachIndexed { index, type ->
            if (lvtIndex == local.index) {
                return index
            }

            lvtIndex += type.size
        }

        return -1
    }

    private fun paramIndex(components: List<RecordComponentNode>, fieldName: String): Int {
        components.forEachIndexed { index, component ->
            if (component.name == fieldName) {
                return index
            }
        }

        return -1
    }

    private fun buildCanonicalConstructorDescriptor(components: List<RecordComponentNode>) = buildString {
        append('(')
        components.forEach { component -> append(component.descriptor) }
        append(")V")
    }

    private fun eligibleConstantOnCursor(gui: GuiView): FieldEntryView? {
        val project = gui.project ?: return null
        val hoveredReference = gui.cursorReference ?: return null

        val field = hoveredReference.entry as? FieldEntryView ?: return null
        if ((project.jarIndex.entryIndex.getAccess(field) and Opcodes.ACC_FINAL) == 0) {
            return null
        }

        return when (field.descriptor) {
            "B", "C", "D", "F", "I", "J", "S", "Ljava/lang/String;", "Ljava/lang/Class;" -> field
            else -> null
        }
    }

    private fun FieldEntryView.toTargetReference() = "target_field ${this.parent.jvmClassName()} ${this.name} ${this.descriptor}"
    private fun MethodEntryView.toTargetReference() = "target_method ${this.parent.jvmClassName()} ${this.name} ${this.descriptor}"

    private fun ClassEntryView.jvmClassName() = this.fullName.replace('/', '.')
}
