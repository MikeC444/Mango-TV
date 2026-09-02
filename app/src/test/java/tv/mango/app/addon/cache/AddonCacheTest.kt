package tv.mango.app.addon.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddonCacheTest {

    @Test
    fun `a cached value is returned before it expires`() {
        var now = 0L
        val cache = AddonCache<String, String>(maxEntries = 10, now = { now })

        cache.put("k", "v", ttlMillis = 1_000)
        now += 999

        assertEquals("v", cache.get("k"))
    }

    @Test
    fun `an expired value is gone`() {
        var now = 0L
        val cache = AddonCache<String, String>(maxEntries = 10, now = { now })

        cache.put("k", "v", ttlMillis = 1_000)
        now += 1_000

        assertNull(cache.get("k"))
    }

    @Test
    fun `a non-positive ttl is never stored`() {
        val cache = AddonCache<String, String>(maxEntries = 10)

        cache.put("k", "v", ttlMillis = 0)

        assertNull(cache.get("k"))
        assertEquals(0, cache.size())
    }

    @Test
    fun `the least recently used entry is evicted once the cache is full`() {
        val cache = AddonCache<String, String>(maxEntries = 2)

        cache.put("a", "1", ttlMillis = 60_000)
        cache.put("b", "2", ttlMillis = 60_000)
        cache.get("a") // touches "a", so "b" is now the least recently used
        cache.put("c", "3", ttlMillis = 60_000)

        assertEquals("1", cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals("3", cache.get("c"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `invalidate removes a single entry`() {
        val cache = AddonCache<String, String>(maxEntries = 10)
        cache.put("a", "1", ttlMillis = 60_000)
        cache.put("b", "2", ttlMillis = 60_000)

        cache.invalidate("a")

        assertNull(cache.get("a"))
        assertEquals("2", cache.get("b"))
    }

    @Test
    fun `clear empties the cache`() {
        val cache = AddonCache<String, String>(maxEntries = 10)
        cache.put("a", "1", ttlMillis = 60_000)

        cache.clear()

        assertEquals(0, cache.size())
    }
}
