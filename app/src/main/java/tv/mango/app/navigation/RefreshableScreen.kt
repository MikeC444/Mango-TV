package tv.mango.app.navigation

/**
 * A screen whose content can be asked to reload.
 *
 * Named for the screen rather than the capability so it does not collide with
 * [tv.mango.app.data.refresh.Refreshable], which is the state a screen is in
 * rather than a thing that can be refreshed.
 *
 * Content is fetched once per launch and then kept, so refreshing is something
 * the viewer asks for rather than something that happens to them. Screens
 * implement this to receive that request; [MainActivity] routes the remote's
 * Menu key to whichever screen is visible.
 */
interface RefreshableScreen {

    /**
     * Reload, without clearing what is on screen.
     *
     * A refresh runs over content the viewer is looking at, so it must not
     * blank it, move their focus, or lose their place.
     */
    fun refresh()
}
