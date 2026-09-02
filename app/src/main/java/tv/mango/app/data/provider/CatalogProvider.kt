package tv.mango.app.data.provider

import tv.mango.app.data.DataResult
import tv.mango.app.models.ContentRow
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType

/**
 * Where content comes from.
 *
 * These interfaces are the application's only opinion about content sourcing.
 * The user interface talks to repositories, repositories talk to these, and
 * nothing above this file knows whether a title arrived from bundled mock data,
 * one catalogue service, or several stitched together. Adding a real backend is
 * a new implementation and one line in the object graph.
 *
 * Named CatalogProvider rather than ContentProvider: the latter is an Android
 * framework class, and shadowing it would make every import site ambiguous.
 */
interface CatalogProvider {

    /** The home screen's rows, in the order they should appear. */
    suspend fun homeRows(): DataResult<List<ContentRow>>

    /**
     * One page of a browsable collection.
     *
     * Paged because a catalogue is unbounded and a Fire Stick's heap is not.
     */
    suspend fun browse(type: MediaType, page: Int, pageSize: Int): DataResult<List<MediaItem>>
}
