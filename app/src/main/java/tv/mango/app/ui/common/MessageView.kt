package tv.mango.app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import tv.mango.app.R

/**
 * Everything the application says when it has nothing to show.
 *
 * Errors, empty results and unavailable sections all land here, so they are
 * phrased the same way: a short statement of what happened in plain words, a
 * line on what to do about it, and a way forward if one exists. No codes, no
 * exception text, nothing the viewer would have to be an engineer to read.
 *
 * The action button is focusable and takes focus automatically, so a viewer
 * holding a remote is never left on a screen with nothing to press.
 */
class MessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val title: TextView
    private val body: TextView
    private val action: Button

    init {
        orientation = VERTICAL
        gravity = Gravity.START
        LayoutInflater.from(context).inflate(R.layout.view_message, this, true)
        title = findViewById(R.id.message_title)
        body = findViewById(R.id.message_body)
        action = findViewById(R.id.message_action)
    }

    fun setMessage(titleRes: Int, bodyRes: Int?) {
        title.setText(titleRes)
        if (bodyRes == null) {
            body.visibility = GONE
        } else {
            body.visibility = VISIBLE
            body.setText(bodyRes)
        }
    }

    fun setMessage(titleText: CharSequence, bodyRes: Int?) {
        title.text = titleText
        if (bodyRes == null) {
            body.visibility = GONE
        } else {
            body.visibility = VISIBLE
            body.setText(bodyRes)
        }
    }

    /** Shows a single action, or hides it when [labelRes] is null. */
    fun setAction(labelRes: Int?, onInvoke: (() -> Unit)? = null) {
        if (labelRes == null || onInvoke == null) {
            action.visibility = GONE
            action.setOnClickListener(null)
            return
        }
        action.visibility = VISIBLE
        action.setText(labelRes)
        action.setOnClickListener { onInvoke() }
    }

    /** Puts focus on the action, so the remote always has somewhere to go. */
    fun focusAction(): Boolean =
        action.visibility == View.VISIBLE && action.requestFocus()
}
