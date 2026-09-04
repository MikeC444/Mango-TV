package tv.mango.app.ui.settings.home

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import tv.mango.app.R
import tv.mango.app.databinding.DialogTextInputBinding
import tv.mango.app.theme.RuntimeTheme
import tv.mango.app.theme.ThemeDrawables
import tv.mango.app.theme.ThemeDrawables.panelRadiusDp

/**
 * A single free-text field over a dim scrim - the one control in Settings ->
 * Home Screen that cannot be a fixed set of LEFT/RIGHT options, because a
 * custom row title is exactly that: whatever a viewer wants to type. The
 * field still takes D-pad focus immediately and the system keyboard is fully
 * navigable by remote, the same as anywhere else in Android a viewer might
 * need to type - this asks nothing extra of Fire TV that typing an add-on's
 * manifest URL (Settings -> Add-ons -> Add Add-on) does not already.
 */
class TextInputDialog(
    context: Context,
    private val titleRes: Int,
    private val initialValue: String,
    private val onSave: (String) -> Unit,
) : Dialog(context, R.style.Theme_Mango_ActionSheet) {

    private lateinit var binding: DialogTextInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogTextInputBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        val colors = RuntimeTheme.colors
        val glass = RuntimeTheme.config.value.glass
        binding.textInputPanel.background = ThemeDrawables.glassPanel(
            colors,
            glass,
            glass.cornerRadius.panelRadiusDp() * context.resources.displayMetrics.density,
        )

        binding.textInputTitle.setText(titleRes)
        binding.textInputField.setText(initialValue)
        binding.textInputField.setSelection(initialValue.length)

        binding.textInputCancel.setOnClickListener { dismiss() }
        binding.textInputSave.setOnClickListener {
            onSave(binding.textInputField.text?.toString().orEmpty())
            dismiss()
        }
        binding.textInputField.setOnEditorActionListener { _, _, _ ->
            onSave(binding.textInputField.text?.toString().orEmpty())
            dismiss()
            true
        }
    }

    override fun show() {
        super.show()
        binding.textInputField.post {
            binding.textInputField.requestFocus()
        }
    }
}
