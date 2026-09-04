package tv.mango.app.ui.settings.home

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.databinding.DialogResetConfirmBinding
import tv.mango.app.di.AppGraph
import tv.mango.app.theme.RuntimeTheme
import tv.mango.app.theme.ThemeDrawables
import tv.mango.app.theme.ThemeDrawables.panelRadiusDp

/**
 * Settings -> Home Screen -> Reset Home Screen's confirmation.
 *
 * [tv.mango.app.repository.HomeScreenConfigRepository.reset] only ever
 * touches the one appearance document this whole package reads and writes -
 * watch history, Continue Watching, the watchlist, add-ons and playback
 * progress each live in their own, entirely separate store, and nothing here
 * can reach them.
 */
class ResetConfirmationDialog(
    context: Context,
    private val scope: CoroutineScope,
) : Dialog(context, R.style.Theme_Mango_ActionSheet) {

    private lateinit var binding: DialogResetConfirmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogResetConfirmBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        val colors = RuntimeTheme.colors
        val glass = RuntimeTheme.config.value.glass
        binding.resetPanel.background = ThemeDrawables.glassPanel(
            colors,
            glass,
            glass.cornerRadius.panelRadiusDp() * context.resources.displayMetrics.density,
        )

        val homeScreenConfigRepository = AppGraph.from(context).homeScreenConfigRepository
        binding.resetCancel.setOnClickListener { dismiss() }
        binding.resetConfirm.setOnClickListener {
            scope.launch { homeScreenConfigRepository.reset() }
            dismiss()
        }
    }

    override fun show() {
        super.show()
        binding.resetCancel.post { binding.resetCancel.requestFocus() }
    }
}
