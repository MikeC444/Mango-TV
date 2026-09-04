import type {
  ContentDetail,
  ContentRow,
  ContentSummary,
  Genre,
  PlaybackInfo,
  SearchFilter,
} from './content';

/**
 * The seam between UI and data. Every screen talks to a ContentProvider —
 * never to mock data, a REST client, or an add-on directly. Swapping mock
 * content for a real backend (or a Stremio-style add-on, once Mango TV's
 * add-on system is wired up) means implementing this interface; nothing
 * above it changes.
 *
 * Methods return an Outcome rather than throwing, so a screen can render a
 * clean Loading / Content / Empty / Error state without try/catch sprinkled
 * through components.
 */
export interface ContentProvider {
  readonly id: string;
  readonly displayName: string;

  getHomeRows(): Promise<Outcome<ContentRow[]>>;
  getFeaturedForHero(): Promise<Outcome<ContentSummary[]>>;
  getDetail(id: string): Promise<Outcome<ContentDetail>>;
  getGenres(): Promise<Outcome<Genre[]>>;
  getByGenre(genreId: string): Promise<Outcome<ContentSummary[]>>;
  getMovies(): Promise<Outcome<ContentSummary[]>>;
  getSeries(): Promise<Outcome<ContentSummary[]>>;
  search(query: string, filter: SearchFilter): Promise<Outcome<ContentSummary[]>>;
  getPlaybackInfo(id: string): Promise<Outcome<PlaybackInfo>>;

  getMyList(): Promise<Outcome<ContentSummary[]>>;
  isInMyList(id: string): Promise<boolean>;
  addToMyList(id: string): Promise<void>;
  removeFromMyList(id: string): Promise<void>;

  getContinueWatching(): Promise<Outcome<ContentSummary[]>>;
  setProgress(id: string, progress: number): Promise<void>;
}

export type Outcome<T> =
  | { status: 'loading' }
  | { status: 'content'; data: T }
  | { status: 'empty' }
  | { status: 'error'; message: string };

export function contentOutcome<T>(data: T, isEmpty: (d: T) => boolean = (d) =>
  Array.isArray(d) && d.length === 0,
): Outcome<T> {
  return isEmpty(data) ? { status: 'empty' } : { status: 'content', data };
}
