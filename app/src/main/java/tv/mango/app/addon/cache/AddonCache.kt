package tv.mango.app.addon.cache

/**
 * A small, bounded, time-limited cache.
 *
 * Two things keep this memory-conscious, which matters on a device with as
 * little heap as a Fire Stick: a fixed ceiling on the number of entries, past
 * which the least recently used is evicted rather than the cache growing
 * without bound, and a time limit per entry rather than one held forever.
 *
 * Access-ordered so eviction is genuinely LRU: a catalogue page a viewer keeps
 * returning to survives being evicted by pages they visited once and moved
 * past.
 */
class AddonCache<K, V>(
    private val maxEntries: Int,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class Entry<V>(val value: V, val expiresAtMillis: Long)

    private val entries = LinkedHashMap<K, Entry<V>>(16, 0.75f, true)

    @Synchronized
    fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (now() >= entry.expiresAtMillis) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    /** A [ttlMillis] of zero or less is a no-op: nothing is cached that should not be. */
    @Synchronized
    fun put(key: K, value: V, ttlMillis: Long) {
        if (ttlMillis <= 0) return
        entries[key] = Entry(value, now() + ttlMillis)
        while (entries.size > maxEntries) {
            val oldest = entries.keys.firstOrNull() ?: break
            entries.remove(oldest)
        }
    }

    @Synchronized
    fun invalidate(key: K) {
        entries.remove(key)
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }

    @Synchronized
    fun size(): Int = entries.size
}
