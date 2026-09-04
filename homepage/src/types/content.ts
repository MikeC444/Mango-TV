/**
 * Normalized domain model for everything the UI renders.
 *
 * These types describe content the way the interface needs it, not the way
 * any particular backend or add-on happens to shape it. A provider's job is
 * to translate its source data into these shapes; nothing above the
 * provider layer ever knows or cares where a title came from.
 */

export type ContentType = 'movie' | 'series';

export interface ArtworkRef {
  /** Opaque key a provider can resolve to a real image at any target size. */
  key: string;
  /** Average color of the artwork, used as a loading placeholder / backdrop wash. */
  dominantColor?: string;
}

export interface Person {
  id: string;
  name: string;
  role?: string;
  photo?: ArtworkRef;
}

export interface Episode {
  id: string;
  seasonNumber: number;
  episodeNumber: number;
  title: string;
  synopsis: string;
  runtimeMinutes: number;
  still: ArtworkRef;
  /** 0–1, watched progress within this episode. */
  progress?: number;
  airDate?: string;
}

export interface Season {
  seasonNumber: number;
  title: string;
  episodeCount: number;
  episodes: Episode[];
}

export interface ContentSummary {
  id: string;
  type: ContentType;
  title: string;
  year: number;
  genres: string[];
  certification: string;
  synopsis: string;
  poster: ArtworkRef;
  backdrop: ArtworkRef;
  rating: number; // 0–10
  /** Movies only. */
  runtimeMinutes?: number;
  /** Series only. */
  seasonCount?: number;
  /** 0–1 watched progress for Continue Watching rows; absent if unstarted. */
  progress?: number;
  /** Which episode "Continue Watching" would resume, for series. */
  resumeEpisode?: { seasonNumber: number; episodeNumber: number; title: string };
}

export interface ContentDetail extends ContentSummary {
  cast: Person[];
  director?: Person;
  creators?: Person[];
  trailerAvailable: boolean;
  seasons?: Season[];
  /** Where this title's data came from — surfaced in Settings/debug, never load-bearing for UI. */
  sourceId: string;
}

export interface Genre {
  id: string;
  name: string;
}

export interface ContentRow {
  id: string;
  title: string;
  items: ContentSummary[];
  /** Continue Watching rows render progress bars and resume affordances. */
  variant?: 'standard' | 'continue-watching';
}

export interface PlaybackSource {
  id: string;
  quality: string;
  url: string;
  addonName: string;
}

export interface SubtitleTrack {
  id: string;
  language: string;
  label: string;
  url: string;
}

export interface PlaybackInfo {
  contentId: string;
  sources: PlaybackSource[];
  subtitles: SubtitleTrack[];
  resumePositionSeconds: number;
}

export type SearchFilter = 'all' | 'movie' | 'series';
