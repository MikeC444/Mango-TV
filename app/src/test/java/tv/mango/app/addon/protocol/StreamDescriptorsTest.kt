package tv.mango.app.addon.protocol

import tv.mango.app.addon.model.StreamQuality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Resolution and codec are not fields in the protocol; add-ons write them into
 * their labels. These pin the conventions actually in use, and pin the
 * behaviour that matters most - that an unrecognised label is left unknown
 * rather than guessed at.
 */
class StreamDescriptorsTest {

    @Test
    fun `resolution is read from the usual spellings`() {
        assertEquals(StreamQuality.UHD_4K, StreamDescriptors.qualityOf("Example 2160p HEVC"))
        assertEquals(StreamQuality.UHD_4K, StreamDescriptors.qualityOf("4K UHD remux"))
        assertEquals(StreamQuality.FHD_1080, StreamDescriptors.qualityOf("1080p x264"))
        assertEquals(StreamQuality.FHD_1080, StreamDescriptors.qualityOf("Full HD"))
        assertEquals(StreamQuality.HD_720, StreamDescriptors.qualityOf("720p"))
        assertEquals(StreamQuality.SD_480, StreamDescriptors.qualityOf("480p"))
    }

    @Test
    fun `an unlabelled stream stays unknown rather than being guessed`() {
        assertEquals(StreamQuality.UNKNOWN, StreamDescriptors.qualityOf("Provider A"))
        assertEquals(StreamQuality.UNKNOWN, StreamDescriptors.qualityOf(""))
    }

    @Test
    fun `codecs are recognised across their spellings`() {
        assertEquals("HEVC", StreamDescriptors.codecOf("2160p HEVC"))
        assertEquals("HEVC", StreamDescriptors.codecOf("x265 rip"))
        assertEquals("H.264", StreamDescriptors.codecOf("1080p h.264"))
        assertEquals("AV1", StreamDescriptors.codecOf("AV1 encode"))
        assertNull(StreamDescriptors.codecOf("1080p"))
    }

    @Test
    fun `sizes are converted to bytes`() {
        assertEquals(1024L * 1024L * 1024L, StreamDescriptors.sizeOf("1 GB"))
        assertEquals(700L * 1024L * 1024L, StreamDescriptors.sizeOf("700MB"))
        assertEquals((1.5 * 1024 * 1024 * 1024).toLong(), StreamDescriptors.sizeOf("1.5 GiB"))
        assertNull(StreamDescriptors.sizeOf("no size here"))
    }

    @Test
    fun `audio channels are read from the usual spellings`() {
        assertEquals("5.1", StreamDescriptors.audioOf("1080p DD5.1"))
        assertEquals("5.1", StreamDescriptors.audioOf("2160p EAC3"))
        assertEquals("7.1", StreamDescriptors.audioOf("1080p 7.1 remux"))
        assertEquals("Dolby Atmos", StreamDescriptors.audioOf("1080p Dolby Atmos"))
        assertEquals("Stereo", StreamDescriptors.audioOf("720p Stereo"))
        assertNull(StreamDescriptors.audioOf("1080p x264"))
    }

    @Test
    fun `quality ranks order the way a viewer would expect`() {
        val sorted = listOf(
            StreamQuality.HD_720,
            StreamQuality.UHD_4K,
            StreamQuality.UNKNOWN,
            StreamQuality.FHD_1080,
        ).sortedByDescending { it.rank }

        assertEquals(
            listOf(
                StreamQuality.UHD_4K,
                StreamQuality.FHD_1080,
                StreamQuality.HD_720,
                StreamQuality.UNKNOWN,
            ),
            sorted,
        )
    }
}
