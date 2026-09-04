import { contentProvider } from '@/data/providerRegistry';
import { useOutcome } from '@/hooks/useOutcome';
import { PageHeader } from '@/components/layout/PageHeader';
import { ContentGrid } from '@/components/content/ContentGrid';
import { LoadingState } from '@/components/common/LoadingState';
import { ErrorState, EmptyState } from '@/components/common/ErrorState';

export function MoviesPage() {
  const outcome = useOutcome(() => contentProvider.getMovies(), []);

  return (
    <>
      <PageHeader title="Movies" />
      {outcome.status === 'loading' && <LoadingState />}
      {outcome.status === 'error' && <ErrorState message={outcome.message} />}
      {outcome.status === 'empty' && <EmptyState title="No movies available" />}
      {outcome.status === 'content' && <ContentGrid items={outcome.data} idPrefix="movies-grid" autoFocusFirst />}
    </>
  );
}
