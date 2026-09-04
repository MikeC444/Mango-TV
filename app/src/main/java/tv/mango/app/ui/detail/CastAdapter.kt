package tv.mango.app.ui.detail

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
import tv.mango.app.models.CastMember
import tv.mango.app.theme.ThemeDefaults

/**
 * The cast row.
 *
 * Every item is focusable. An earlier version was not, on the reasoning that a
 * cast list is information rather than a set of destinations - but the detail
 * screen scrolls only to follow focus, so with nothing focusable below them
 * these rows could never be brought on screen at all. Unreachable content is a
 * worse outcome than a few extra stops on the way down, and a focusable cast
 * row is the convention on this platform in any case.
 *
 * Selecting a cast member does nothing yet. The row exists to be read.
 */
class CastAdapter : RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    private var members: List<CastMember> = emptyList()

    /** A cast list arrives whole, with the title it belongs to. */
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newMembers: List<CastMember>) {
        members = newMembers
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = members.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder =
        CastViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_cast, parent, false),
        )

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        holder.bind(members[position])
    }

    class CastViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        private val name: TextView = view.findViewById(R.id.cast_name)
        private val role: TextView = view.findViewById(R.id.cast_role)

        private val colorActive = ThemeDefaults.colors.primaryText
        private val colorResting = ThemeDefaults.colors.secondaryText

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // The platform's default highlight is a flat grey rectangle.
                view.defaultFocusHighlightEnabled = false
            }
            // Brightness alongside the raised surface, so focus is never
            // carried by one cue on its own.
            view.setOnFocusChangeListener { _, hasFocus ->
                role.setTextColor(if (hasFocus) colorActive else colorResting)
            }
        }

        fun bind(member: CastMember) {
            name.text = member.name
            role.text = member.role
            role.setTextColor(if (view.hasFocus()) colorActive else colorResting)
            // Read as one phrase rather than as two disconnected labels.
            view.contentDescription =
                view.context.getString(R.string.cd_cast_member, member.name, member.role)
        }
    }
}
