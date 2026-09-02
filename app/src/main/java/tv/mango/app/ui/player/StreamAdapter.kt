package tv.mango.app.ui.player

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
import tv.mango.app.addon.model.StreamQuality
import tv.mango.app.addon.model.StreamResult
import tv.mango.app.utilities.Formatters

/** Every source found for the title, ranked. Selecting an unsupported one does nothing - it cannot be focused. */
class StreamAdapter(
    private val onSelected: (StreamResult) -> Unit,
) : RecyclerView.Adapter<StreamAdapter.StreamViewHolder>() {

    private var streams: List<StreamResult> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(newStreams: List<StreamResult>) {
        streams = newStreams
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = streams.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder =
        StreamViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_stream, parent, false),
            onSelected,
        )

    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        holder.bind(streams[position])
    }

    class StreamViewHolder(
        private val view: View,
        private val onSelected: (StreamResult) -> Unit,
    ) : RecyclerView.ViewHolder(view) {

        private val quality: TextView = view.findViewById(R.id.stream_quality)
        private val details: TextView = view.findViewById(R.id.stream_details)
        private val unsupported: TextView = view.findViewById(R.id.stream_unsupported)

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                view.defaultFocusHighlightEnabled = false
            }
        }

        fun bind(stream: StreamResult) {
            quality.text = qualityLabel(stream)
            details.text = detailsLine(stream)

            val supported = stream.isDirectlyPlayable
            if (!supported) {
                // Most real-world add-ons only offer a torrent source - naming
                // that plainly is more useful than a blanket "not supported"
                // that reads like something this application got wrong.
                unsupported.setText(
                    if (stream.isPeerToPeer) {
                        R.string.stream_picker_torrent_unsupported
                    } else {
                        R.string.stream_picker_not_supported
                    },
                )
            }
            unsupported.visibility = if (supported) View.GONE else View.VISIBLE
            view.isFocusable = supported
            view.isClickable = supported
            view.alpha = if (supported) 1f else 0.5f
            if (supported) {
                view.setOnClickListener { onSelected(stream) }
            } else {
                view.setOnClickListener(null)
            }
        }

        private fun qualityLabel(stream: StreamResult): String {
            if (stream.quality != StreamQuality.UNKNOWN) return stream.quality.label
            return stream.name?.takeIf { it.isNotBlank() }
                ?: stream.title?.takeIf { it.isNotBlank() }
                ?: view.context.getString(R.string.stream_quality_unknown)
        }

        private fun detailsLine(stream: StreamResult): String = listOfNotNull(
            stream.providerName.takeIf { it.isNotBlank() },
            stream.codec,
            stream.language?.uppercase(),
            stream.sizeBytes?.let(Formatters::fileSize),
        ).joinToString(Formatters.SEPARATOR)
    }
}
