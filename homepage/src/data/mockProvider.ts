import type { ContentDetail, ContentRow, ContentSummary, Genre, PlaybackInfo, SearchFilter } from '@/types/content';
import type { ContentProvider, Outcome } from '@/types/provider';
import { contentOutcome } from '@/types/provider';
import { ALL_TITLES, MOVIES, SERIES, titleById } from './mock/titles';
import { buildHomeRows, heroFeatured, toDetail, toSummary } from './mock/rows';
import { GENRES } from './mock/genres';
import { getMyListIds, setMyListIds, getProgressMap, setProgressMap } from './local/storage';

const NETWORK_DELAY_MS = 120;

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), NETWORK_DELAY_MS));
}

/**
 * Bundled mock content, implementing the exact same ContentProvider seam a
 * real backend or a Stremio-style add-on would. Nothing in `components/` or
 * `pages/` imports from here directly — everything goes through
 * `providerRegistry`, so this file is the only thing that needs to change
 * to point the app at real data.
 */
export class MockContentProvider implements ContentProvider {
  readonly id = 'mango.mock';
  readonly displayName = 'Mango Sample Library';

  private continueWatchingIds(): string[] {
    const progress = getProgressMap();
    return Object.entries(progress)
      .filter(([, v]) => v > 0 && v < 0.98)
      .sort((a, b) => b[1] - a[1])
      .map(([id]) => id);
  }

  async getHomeRows(): Promise<Outcome<ContentRow[]>> {
    const rows = buildHomeRows(this.continueWatchingIds());
    return delay(contentOutcome(rows));
  }

  async getFeaturedForHero(): Promise<Outcome<ContentSummary[]>> {
    return delay(contentOutcome(heroFeatured()));
  }

  async getDetail(id: string): Promise<Outcome<ContentDetail>> {
    const t = titleById(id);
    if (!t) return delay({ status: 'error', message: `No title with id "${id}".` });
    return delay({ status: 'content', data: toDetail(t) as ContentDetail });
  }

  async getGenres(): Promise<Outcome<Genre[]>> {
    return delay(contentOutcome(GENRES));
  }

  async getByGenre(genreId: string): Promise<Outcome<ContentSummary[]>> {
    const items = ALL_TITLES.filter((t) => t.genres.includes(genreId)).map(toSummary);
    return delay(contentOutcome(items));
  }

  async getMovies(): Promise<Outcome<ContentSummary[]>> {
    return delay(contentOutcome(MOVIES.map(toSummary)));
  }

  async getSeries(): Promise<Outcome<ContentSummary[]>> {
    return delay(contentOutcome(SERIES.map(toSummary)));
  }

  async search(query: string, filter: SearchFilter): Promise<Outcome<ContentSummary[]>> {
    const q = query.trim().toLowerCase();
    if (!q) return delay({ status: 'empty' });
    const pool = filter === 'all' ? ALL_TITLES : ALL_TITLES.filter((t) => t.type === filter);
    const results = pool.filter(
      (t) =>
        t.title.toLowerCase().includes(q) ||
        t.genres.some((g) => g.includes(q)) ||
        t.synopsis.toLowerCase().includes(q),
    );
    return delay(contentOutcome(results.map(toSummary)));
  }

  async getPlaybackInfo(id: string): Promise<Outcome<PlaybackInfo>> {
    const t = titleById(id);
    if (!t) return delay({ status: 'error', message: `No title with id "${id}".` });
    const progress = getProgressMap();
    const info: PlaybackInfo = {
      contentId: id,
      sources: [
        { id: `${id}-src-1080p`, quality: '1080p', url: `mango://mock/${id}/1080p`, addonName: 'Mango Sample Library' },
        { id: `${id}-src-720p`, quality: '720p', url: `mango://mock/${id}/720p`, addonName: 'Mango Sample Library' },
      ],
      subtitles: [
        { id: `${id}-sub-en`, language: 'en', label: 'English', url: `mango://mock/${id}/subs/en.vtt` },
      ],
      resumePositionSeconds: Math.round((progress[id] ?? 0) * (t.runtimeMinutes ?? 45) * 60),
    };
    return delay({ status: 'content', data: info });
  }

  async getMyList(): Promise<Outcome<ContentSummary[]>> {
    const ids = getMyListIds();
    const items = ids.map((id) => titleById(id)).filter((t): t is NonNullable<typeof t> => !!t).map(toSummary);
    return delay(contentOutcome(items));
  }

  async isInMyList(id: string): Promise<boolean> {
    return getMyListIds().includes(id);
  }

  async addToMyList(id: string): Promise<void> {
    const ids = getMyListIds();
    if (!ids.includes(id)) setMyListIds([id, ...ids]);
  }

  async removeFromMyList(id: string): Promise<void> {
    setMyListIds(getMyListIds().filter((x) => x !== id));
  }

  async getContinueWatching(): Promise<Outcome<ContentSummary[]>> {
    const items = this.continueWatchingIds()
      .map((id) => titleById(id))
      .filter((t): t is NonNullable<typeof t> => !!t)
      .map(toSummary);
    return delay(contentOutcome(items));
  }

  async setProgress(id: string, progress: number): Promise<void> {
    const map = getProgressMap();
    map[id] = Math.max(0, Math.min(1, progress));
    setProgressMap(map);
  }
}
