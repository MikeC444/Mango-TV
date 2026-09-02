package tv.mango.app.cache

/**
 * Turns an artwork key into something the image loader can fetch.
 *
 * Content models carry keys ("poster_m03"), never URLs. That keeps the
 * catalogue independent of where its images happen to live, and it is what
 * makes swapping bundled artwork for a content delivery network a change to
 * one implementation of this interface.
 *
 * A remote implementation would also fold the requested size into the URL, so
 * the network delivers a poster-sized image rather than a full-resolution one
 * that the device then has to shrink.
 */
interface ArtworkSource {
    fun uriFor(key: String, widthPx: Int, heightPx: Int): String
}

/**
 * Artwork bundled in the APK.
 *
 * Loaded through the same image pipeline as anything remote would be, so the
 * caching, sizing and recycling behaviour being exercised during development
 * is the behaviour that will run in production.
 */
class BundledArtworkSource : ArtworkSource {

    override fun uriFor(key: String, widthPx: Int, heightPx: Int): String =
        "file:///android_asset/artwork/$key.webp"
}
