import type { ContentRow, ContentSummary } from '@/types/content';
import { GENRES } from './genres';
import { MOVIES, SERIES, ALL_TITLES, resolveCast, resolveDirector, resolveCreators, type TitleRecord } from './titles';

export function toSummary(t: TitleRecord): ContentSummary {
  return {
    id: t.id,
    type: t.type,
    title: t.title,
    year: t.year,
    genres: t.genres,
    certification: t.certification,
    synopsis: t.synopsis,
    poster: t.poster,
    backdrop: t.backdrop,
    rating: t.rating,
    runtimeMinutes: t.runtimeMinutes,
    seasonCount: t.seasonCount,
    progress: t.progress,
    resumeEpisode: t.resumeEpisode,
  };
}

export function toDetail(t: TitleRecord) {
  return {
    ...toSummary(t),
    cast: resolveCast(t),
    director: resolveDirector(t),
    creators: resolveCreators(t),
    trailerAvailable: t.trailerAvailable,
    seasons: t.seasons,
    sourceId: t.sourceId,
  };
}

const byRating = (a: TitleRecord, b: TitleRecord) => b.rating - a.rating;
const byYear = (a: TitleRecord, b: TitleRecord) => b.year - a.year;

function pick(list: TitleRecord[], ids: string[]): TitleRecord[] {
  return ids.map((id) => list.find((t) => t.id === id)).filter((t): t is TitleRecord => !!t);
}

export function buildHomeRows(continueWatchingIds: string[]): ContentRow[] {
  const continueWatching = pick(ALL_TITLES, continueWatchingIds);
  const trending = [...ALL_TITLES].sort(byRating).slice(0, 12);
  const popularMovies = [...MOVIES].sort(byRating).slice(0, 12);
  const popularSeries = [...SERIES].sort(byRating).slice(0, 10);
  const recentlyAdded = [...ALL_TITLES].sort(byYear).slice(0, 12);
  const topRated = [...ALL_TITLES].filter((t) => t.rating >= 7.8).sort(byRating).slice(0, 12);

  const rows: ContentRow[] = [];

  if (continueWatching.length > 0) {
    rows.push({
      id: 'continue-watching',
      title: 'Continue Watching',
      items: continueWatching.map(toSummary),
      variant: 'continue-watching',
    });
  }

  rows.push(
    { id: 'trending', title: 'Trending Now', items: trending.map(toSummary) },
    { id: 'popular-movies', title: 'Popular Movies', items: popularMovies.map(toSummary) },
    { id: 'popular-series', title: 'Popular TV Shows', items: popularSeries.map(toSummary) },
    { id: 'recently-added', title: 'Recently Added', items: recentlyAdded.map(toSummary) },
    { id: 'top-rated', title: 'Top Rated', items: topRated.map(toSummary) },
  );

  for (const g of GENRES) {
    const items = ALL_TITLES.filter((t) => t.genres.includes(g.id)).sort(byRating);
    if (items.length >= 4) {
      rows.push({ id: `genre-${g.id}`, title: g.name, items: items.map(toSummary) });
    }
  }

  return rows;
}

export function heroFeatured(): ContentSummary[] {
  const ids = ['sr03', 'mv04', 'sr01', 'mv13', 'mv15', 'sr08'];
  return pick(ALL_TITLES, ids).map(toSummary);
}
