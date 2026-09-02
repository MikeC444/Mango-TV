package tv.mango.app.ui.detail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
import tv.mango.app.models.CastMember

/**
 * The cast row.
 *
 * Nothing here is focusable. A cast list is information, not a set of places to
 * go, and making it focusable would put a dozen dead stops in the path between
 * the actions and the episodes below.
 */
class CastAdapter : RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    private var members: List<CastMember> = emptyList()

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

    class CastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.cast_name)
        private val role: TextView = view.findViewById(R.id.cast_role)

        fun bind(member: CastMember) {
            name.text = member.name
            role.text = member.role
        }
    }
}
