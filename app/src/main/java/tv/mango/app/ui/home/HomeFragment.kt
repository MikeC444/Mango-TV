package tv.mango.app.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS
import android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.cache.ImageLoader
import tv.mango.app.data.FailureReason
import tv.mango.app.data.UiState
import tv.mango.app.databinding.FragmentHomeBinding
import tv.mango.app.di.appGraph
import tv.mango.app.models.Episode
import tv.mango.app.models.HomeContent
import tv.mango.app.models.MediaItem
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.settings.home.BackgroundConfig
import tv.mango.app.settings.home.BackgroundType
import tv.mango.app.settings.home.ContentWidth
import tv.mango.app.settings.home.HeroArtworkMode
import tv.mango.app.settings.home.HeroConfig
import tv.mango.app.settings.home.HeroRotation
import tv.mango.app.settings.home.HeroSize
import tv.mango.app.settings.home.HomeScreenConfig
import tv.mango.app.theme.RuntimeTheme
import tv.mango.app.ui.core.CardActionSheet
import tv.mango.app.ui.core.ContentRowsAdapter

/**
 * The home screen: a cinematic hero, and rows of content beneath it.
 *
 * Two behaviours here are worth explaining, because both exist to keep the
 * screen from doing work it does not need to do.
 *
 * **The hero follows focus, but only while it can be seen.** Moving along a row
 * changes what the hero is describing, which makes browsing feel like reading
 * rather than like picking from a grid. Once the rows have scrolled up over the
 * hero, focus changes stop touching it entirely - no text updates, and above
 * all no backdrop decode for an image nobody is looking at. The pending title
 * is remembered, so scrolling back up shows the right one immediately.
 *
 * **Updates are debounced.** Holding a direction on the remote crosses a dozen
 * cards a second. Without the delay that would be a dozen backdrop loads,
 * eleven of them cancelled before they finished.
 *
 * A screen's whole appearance - hero, background, layout - is read from
 * [RuntimeTheme], re-applied on every state emission (see [showContent]) so a
 * viewer changing it in Settings -> Home Screen sees the new look the moment
 * they navigate back here - [tv.mango.app.navigation.Navigator] never keeps an
 * old Home behind Settings on the back stack - and so a restart never shows the
 * built-in defaults while [tv.mango.app.repository.HomeScreenConfigRepository]'s
 * first read is still in flight.
 */
class HomeFragment : Fragment() {

    private var binding: FragmentHomeBinding? = null

    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer {
                HomeViewModel(
                    appGraph.catalogRepository,
                    appGraph.libraryRepository,
                    appGraph.homeScreenConfigRepository,
                )
            }
        }
    }

    private val rowsAdapter = ContentRowsAdapter(
        onItemSelected = ::onCardSelected,
        onItemFocused = ::onCardFocused,
        onItemLongSelected = ::onCardLongPressed,
    )

    /**
     * A getter, not a one-time read: [tv.mango.app.repository.HomeScreenConfigRepository]'s
     * first value can still be loading from DataStore the moment this fragment
     * is first built at a cold start, before it has caught up with whatever a
     * viewer saved last session. [showContent] re-applies this on every state
     * emission - including the one that arrives the instant the real
     * configuration finishes loading - so a restart never shows the built-in
     * defaults for longer than that first frame.
     */
    private val config: HomeScreenConfig get() = RuntimeTheme.config.value
    private val heroConfig: HeroConfig get() = config.hero

    /** How far the rows have been scrolled up over the hero, in pixels. */
    private var scrolledBy = 0

    /** The title the hero should be showing once it is visible again. */
    private var pendingHeroItem: MediaItem? = null

    /** What Hero Rotation cycles through while the hero holds focus and nothing has been picked. */
    private var rotationCandidates: List<MediaItem> = emptyList()
    private var rotationIndex = 0

    private val applyHeroItem = Runnable {
        val item = pendingHeroItem ?: return@Runnable
        if (isHeroVisible()) binding?.hero?.show(item)
    }

    private val rotateHero = Runnable { rotateHeroItem() }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (!heroConfig.enabled) return
            scrolledBy = (scrolledBy + dy).coerceAtLeast(0)
            updateHeroForScroll()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentHomeBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        views.rows.adapter = rowsAdapter
        views.rows.addOnScrollListener(scrollListener)

        applyHeroLayout(views)
        applyBackground(config.background, item = null)

        // Not just requestPlayback(item): the hero can be showing a Continue
        // Watching card the viewer scrolled to, and pressing Play there has
        // to resume the exact episode too, the same as tapping the card
        // itself would.
        views.hero.onPlay = ::playItem
        views.hero.onDetails = ::openDetail

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    /** Sizes the hero (or removes it) and insets the rows to match - Settings -> Home Screen -> Hero and Home Layout. */
    private fun applyHeroLayout(views: FragmentHomeBinding) {
        val heroHeight = if (heroConfig.enabled) {
            views.hero.visibility = View.VISIBLE
            views.hero.applyConfig(heroConfig)
            val widthRes = when (config.layout.contentWidth) {
                ContentWidth.STANDARD -> R.dimen.hero_text_max_width
                ContentWidth.WIDE -> R.dimen.hero_text_max_width_wide
                ContentWidth.MAXIMUM -> R.dimen.hero_text_max_width_maximum
            }
            views.hero.applyContentWidth(resources.getDimensionPixelSize(widthRes))
            resources.getDimensionPixelSize(
                when (heroConfig.size) {
                    HeroSize.COMPACT -> R.dimen.hero_height_compact
                    HeroSize.NORMAL -> R.dimen.hero_height
                    HeroSize.LARGE -> R.dimen.hero_height_large
                },
            )
        } else {
            views.hero.visibility = View.GONE
            resources.getDimensionPixelSize(R.dimen.safe_area_vertical)
        }

        views.rows.setPadding(
            0,
            heroHeight,
            0,
            resources.getDimensionPixelSize(R.dimen.safe_area_vertical),
        )
    }

    /**
     * Settings -> Home Screen -> Background. Solid, the default, never touches
     * either overlay view - the window's own base surface is already exactly
     * that. Everything else is a cheap flat drawable or the current hero
     * artwork itself; never a real-time blur, the same rule every glass
     * surface in the application follows.
     */
    private fun applyBackground(background: BackgroundConfig, item: MediaItem?) {
        val views = binding ?: return
        val colors = RuntimeTheme.colors
        val dim = (1f - background.brightness).coerceIn(0f, 1f)

        when (background.type) {
            BackgroundType.SOLID -> {
                views.backgroundArtwork.alpha = 0f
                views.backgroundScrim.setBackgroundColor(Color.argb((dim * 255).toInt(), 0, 0, 0))
            }
            BackgroundType.GRADIENT, BackgroundType.CINEMATIC -> {
                views.backgroundArtwork.alpha = 0f
                val strength = if (background.type == BackgroundType.CINEMATIC) {
                    (background.gradientStrength * 1.3f).coerceAtMost(1f)
                } else {
                    background.gradientStrength
                }
                views.backgroundScrim.background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        ColorUtils.setAlphaComponent(colors.secondaryBackground, (strength * 255).toInt()),
                        ColorUtils.setAlphaComponent(colors.primaryBackground, ((strength + dim).coerceAtMost(1f) * 255).toInt()),
                    ),
                )
            }
            BackgroundType.DYNAMIC_ARTWORK, BackgroundType.BLURRED_ARTWORK -> {
                val key = item?.images?.backdrop
                if (key != null) {
                    ImageLoader.loadBackdrop(views.backgroundArtwork, key, BACKGROUND_ART_WIDTH_PX, BACKGROUND_ART_HEIGHT_PX)
                    views.backgroundArtwork.alpha = background.artworkVisibility
                } else {
                    views.backgroundArtwork.alpha = 0f
                }
                // "Blurred" steps the same cheap stand-in every glass surface
                // uses: a stronger darkening overlay in place of an actual blur
                // pass over the artwork.
                val blurBoost = if (background.type == BackgroundType.BLURRED_ARTWORK) background.blurStrength else 0f
                val scrimAlpha = (dim + blurBoost).coerceIn(0f, 1f)
                views.backgroundScrim.setBackgroundColor(Color.argb((scrimAlpha * 255).toInt(), 0, 0, 0))
            }
        }
    }

    private fun render(state: UiState<HomeContent>) {
        val views = binding ?: return
        when (state) {
            is UiState.Loading -> {
                // Deliberately blank. A spinner would be a continuous animation
                // on a screen about to fill in a moment; the charcoal surface is
                // a calmer wait than a moving one.
                views.rows.visibility = View.GONE
                views.hero.visibility = View.INVISIBLE
                views.message.visibility = View.GONE
            }

            is UiState.Content -> showContent(state.value)

            is UiState.Empty -> showMessage(R.string.error_empty_title, null)

            is UiState.Error -> when (state.reason) {
                FailureReason.NETWORK ->
                    showMessage(R.string.error_network_title, R.string.error_network_body)
                else ->
                    showMessage(R.string.error_generic_title, R.string.error_generic_body)
            }
        }
    }

    private fun showContent(content: HomeContent) {
        val views = binding ?: return
        views.message.visibility = View.GONE
        views.rows.visibility = View.VISIBLE

        applyHeroLayout(views)
        rowsAdapter.submit(content.rows)

        if (heroConfig.enabled) {
            views.hero.visibility = View.VISIBLE
            pendingHeroItem = content.featured
            views.hero.show(content.featured)
            applyBackground(config.background, content.featured)

            buildRotationCandidates(content)
            scheduleRotation()

            // Focus has to land somewhere the instant content appears, or the
            // first press of the remote does nothing. The hero's primary
            // action is the most useful place for it to be.
            views.hero.post { views.hero.focusPrimaryAction() }
        } else {
            views.rows.post { views.rows.requestFocus() }
        }
    }

    /** Rotation cycles the first non-Continue-Watching row - the closest thing this catalogue has to "featured". */
    private fun buildRotationCandidates(content: HomeContent) {
        rotationCandidates = content.rows
            .firstOrNull { it.id != CONTINUE_WATCHING_ROW_ID }
            ?.items
            ?.take(ROTATION_CANDIDATE_LIMIT)
            .orEmpty()
        rotationIndex = 0
    }

    private fun scheduleRotation() {
        val views = binding ?: return
        views.hero.removeCallbacks(rotateHero)
        val interval = heroConfig.rotation.intervalMillis() ?: return
        if (rotationCandidates.size < 2) return
        views.hero.postDelayed(rotateHero, interval)
    }

    /**
     * Advances to the next rotation candidate, but only while the hero itself
     * holds focus - the moment a viewer moves into a row, focus leaves the
     * hero and rotation stops touching it, exactly the "pause while
     * navigating" behaviour Settings -> Home Screen -> Hero promises.
     */
    private fun rotateHeroItem() {
        val views = binding ?: return
        if (!views.hero.hasFocus() || !isHeroVisible() || rotationCandidates.isEmpty()) {
            scheduleRotation()
            return
        }
        rotationIndex = (rotationIndex + 1) % rotationCandidates.size
        val next = rotationCandidates[rotationIndex]
        pendingHeroItem = next
        views.hero.show(next)
        applyBackground(config.background, next)
        scheduleRotation()
    }

    private fun onCardFocused(item: MediaItem) {
        pendingHeroItem = item
        val views = binding ?: return
        if (!heroConfig.enabled || heroConfig.artworkMode == HeroArtworkMode.STATIC) return
        views.hero.removeCallbacks(applyHeroItem)
        if (isHeroVisible()) {
            views.hero.postDelayed(applyHeroItem, HERO_DEBOUNCE_MS)
        }
    }

    /**
     * Fades the hero out as the rows climb over it, and takes it out of the
     * focus path once it is gone - a viewer pressing up should never land on an
     * invisible button.
     */
    private fun updateHeroForScroll() {
        val views = binding ?: return
        val fadeOver = resources.getDimensionPixelSize(R.dimen.hero_fade_distance).toFloat()
        val alpha = (1f - scrolledBy / fadeOver).coerceIn(0f, 1f)
        views.hero.alpha = alpha

        val visible = alpha > VISIBILITY_THRESHOLD
        val wanted = if (visible) View.VISIBLE else View.INVISIBLE
        if (views.hero.visibility != wanted) {
            views.hero.visibility = wanted
            // Coming back into view, it may be describing a title the viewer
            // moved away from several rows ago.
            if (visible) pendingHeroItem?.let(views.hero::show)
        }

        // Reachability is stricter than the fade: the rows begin drawing over
        // the hero from the first pixel of scroll, well before it is faint
        // enough to cross the visibility threshold above, so its buttons must
        // stop being reachable immediately - a viewer navigating a row should
        // never have focus land on a Play or Details button they can no
        // longer clearly see underneath it.
        views.hero.descendantFocusability =
            if (scrolledBy == 0) FOCUS_BEFORE_DESCENDANTS else FOCUS_BLOCK_DESCENDANTS
    }

    private fun isHeroVisible(): Boolean =
        heroConfig.enabled &&
            binding?.hero?.let { it.visibility == View.VISIBLE && it.alpha > VISIBILITY_THRESHOLD } == true

    private fun showMessage(titleRes: Int, bodyRes: Int?) {
        val views = binding ?: return
        views.rows.visibility = View.GONE
        views.hero.visibility = View.INVISIBLE
        views.message.visibility = View.VISIBLE
        views.message.setMessage(titleRes, bodyRes)
        views.message.setAction(R.string.action_retry) { viewModel.retry() }
        views.message.post { views.message.focusAction() }
    }

    private fun openDetail(item: MediaItem) {
        (activity as? NavigationHost)?.openDetail(item)
    }

    /**
     * A Continue Watching card resumes directly rather than opening the
     * detail screen first - "click it and it plays" is the entire point of
     * the row. Every other card behaves as it always has.
     */
    private fun onCardSelected(item: MediaItem) {
        if (item.resume == null) openDetail(item) else playItem(item)
    }

    /** Resumes exactly where a Continue Watching card's snapshot says it left off. */
    private fun playItem(item: MediaItem) {
        val resume = item.resume
        val episode = resume?.episodeId?.let { episodeId ->
            Episode(
                id = episodeId,
                seriesId = item.id,
                season = resume.episodeSeason ?: 1,
                number = resume.episodeNumber ?: 1,
                title = resume.episodeTitle ?: item.title,
                // Never shown: the picker and player describe the title by
                // its own artwork, not the episode's.
                thumbnail = item.images.poster,
            )
        }
        (activity as? NavigationHost)?.requestPlayback(item, episode, startFromBeginning = false)
    }

    /**
     * The long-press menu's Play action - unlike a tap, it never opens Detail
     * first. That trip is exactly what the menu exists to skip.
     */
    private fun playFromSheet(item: MediaItem) {
        if (item.resume != null) playItem(item) else (activity as? NavigationHost)?.requestPlayback(item)
    }

    /** Every long press opens the same quick-action menu; only a Continue Watching card gets an extra row. */
    private fun onCardLongPressed(item: MediaItem, anchor: View): Boolean {
        val host = activity as? NavigationHost ?: return false
        CardActionSheet(
            context = requireContext(),
            item = item,
            anchor = anchor,
            library = appGraph.libraryRepository,
            scope = viewLifecycleOwner.lifecycleScope,
            onPlay = ::playFromSheet,
            onDetails = host::openDetail,
            onFindSimilar = host::findSimilar,
            onRemoveFromContinueWatching = item.resume?.let { resume ->
                {
                    viewModel.removeFromContinueWatching(resume.id)
                    Toast.makeText(requireContext(), R.string.continue_watching_removed, Toast.LENGTH_SHORT).show()
                }
            },
        ).show()
        return true
    }

    override fun onDestroyView() {
        binding?.let {
            it.hero.removeCallbacks(applyHeroItem)
            it.hero.removeCallbacks(rotateHero)
            it.rows.removeOnScrollListener(scrollListener)
            ImageLoader.clear(it.backgroundArtwork)
            // The adapter outlives the view; leaving it attached would keep the
            // whole hierarchy alive behind it.
            it.rows.adapter = null
        }
        binding = null
        super.onDestroyView()
    }

    private fun HeroRotation.intervalMillis(): Long? = when (this) {
        HeroRotation.OFF -> null
        HeroRotation.SEC_10 -> 10_000L
        HeroRotation.SEC_15 -> 15_000L
        HeroRotation.SEC_20 -> 20_000L
        HeroRotation.SEC_30 -> 30_000L
    }

    private companion object {
        /** Long enough to outlast held-down navigation, short enough to feel immediate. */
        const val HERO_DEBOUNCE_MS = 220L

        /** Below this the hero is effectively gone and stops taking focus. */
        const val VISIBILITY_THRESHOLD = 0.05f

        const val ROTATION_CANDIDATE_LIMIT = 6
        const val CONTINUE_WATCHING_ROW_ID = "continue_watching"

        const val BACKGROUND_ART_WIDTH_PX = 960
        const val BACKGROUND_ART_HEIGHT_PX = 540
    }
}
