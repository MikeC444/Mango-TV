package tv.mango.app.data.refresh

import tv.mango.app.data.FailureReason
import tv.mango.app.data.UiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The rule that stops a refresh blanking a working screen.
 *
 * The data layer emits the same Loading-then-result sequence for a first load
 * and for a refresh, so everything here turns on whether content is already
 * held.
 */
class RefreshFoldTest {

    private fun content(value: String) = UiState.Content(value)

    private fun loaded(value: String): Refreshable<String> =
        RefreshFold.next(RefreshFold.initial(), content(value))

    // ------------------------------------------------------------ first load

    @Test
    fun `a first load shows loading, because there is nothing to keep`() {
        val state = RefreshFold.next(RefreshFold.initial<String>(), UiState.Loading)

        assertIs<UiState.Loading>(state.content)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `content replaces the loading state`() {
        val state = loaded("rows")

        assertEquals(content("rows"), state.content)
        assertFalse(state.isRefreshing)
        assertFalse(state.refreshFailed)
    }

    // -------------------------------------------------------------- refresh

    @Test
    fun `a refresh keeps the content on screen rather than blanking it`() {
        // The emission that used to hide the rows and the hero.
        val refreshing = RefreshFold.next(loaded("rows"), UiState.Loading)

        assertEquals(content("rows"), refreshing.content)
        assertTrue(refreshing.isRefreshing)
    }

    @Test
    fun `a completed refresh swaps the content in and stops refreshing`() {
        val refreshing = RefreshFold.next(loaded("old"), UiState.Loading)
        val done = RefreshFold.next(refreshing, content("new"))

        assertEquals(content("new"), done.content)
        assertFalse(done.isRefreshing)
    }

    @Test
    fun `a failed refresh leaves the working screen alone and says so`() {
        val refreshing = RefreshFold.next(loaded("rows"), UiState.Loading)
        val failed = RefreshFold.next(refreshing, UiState.Error(FailureReason.NETWORK))

        // The viewer keeps the screen they were looking at.
        assertEquals(content("rows"), failed.content)
        assertTrue(failed.refreshFailed)
        assertFalse(failed.isRefreshing)
    }

    @Test
    fun `a later successful refresh clears the earlier failure`() {
        val failed = RefreshFold.next(loaded("rows"), UiState.Error(FailureReason.NETWORK))
        val recovered = RefreshFold.next(failed, content("fresh"))

        assertEquals(content("fresh"), recovered.content)
        assertFalse(recovered.refreshFailed)
    }

    @Test
    fun `starting another refresh clears the previous failure`() {
        val failed = RefreshFold.next(loaded("rows"), UiState.Error(FailureReason.NETWORK))
        val retrying = RefreshFold.next(failed, UiState.Loading)

        assertFalse(retrying.refreshFailed)
        assertTrue(retrying.isRefreshing)
    }

    // ------------------------------------------------------------- failures

    @Test
    fun `an error with nothing to fall back on becomes the screen`() {
        val state = RefreshFold.next(
            RefreshFold.initial<String>(),
            UiState.Error(FailureReason.NETWORK),
        )

        assertIs<UiState.Error>(state.content)
        assertEquals(FailureReason.NETWORK, RefreshFold.failureReasonOf(state))
        // Nothing was kept, so this is not a failed refresh - it is a failure.
        assertFalse(state.refreshFailed)
    }

    @Test
    fun `an empty result replaces content rather than being kept`() {
        // Empty is an answer about the catalogue, not a transport failure: a
        // provider whose catalogue has emptied should stop showing titles it no
        // longer offers.
        val state = RefreshFold.next(loaded("rows"), UiState.Empty)

        assertIs<UiState.Empty>(state.content)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `repeated refreshes over content never surface loading`() {
        var state = loaded("rows")
        repeat(5) {
            state = RefreshFold.next(state, UiState.Loading)
            assertEquals(content("rows"), state.content)
        }
    }
}
