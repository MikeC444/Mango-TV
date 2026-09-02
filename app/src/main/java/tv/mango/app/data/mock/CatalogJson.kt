package tv.mango.app.data.mock

import kotlinx.serialization.Serializable

/**
 * The wire shape of the bundled catalogue.
 *
 * Kept separate from the domain models on purpose. A serialisation schema
 * belongs to whoever produces the data and changes when they change it;
 * the domain models belong to this application. Keeping the two apart means a
 * provider can absorb a change in its source's field names without every
 * screen having to hear about it - which is the whole point of the provider
 * abstraction.
 */
@Serializable
internal data class CatalogJson(
    val titles: List<TitleJson>,
    val rows: List<RowJson>,
    val featured: List<String>,
)

@Serializable
internal data class TitleJson(
    val id: String,
    val type: String,
    val title: String,
    val year: Int,
    val runtimeMinutes: Int,
    val certification: String,
    val genres: List<String>,
    val synopsis: String,
    val progress: Float = 0f,
)

@Serializable
internal data class RowJson(
    val id: String,
    val title: String,
    val titleIds: List<String>,
)
