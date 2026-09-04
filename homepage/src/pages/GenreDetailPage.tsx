import { useParams } from 'react-router-dom';
import { contentProvider } from '@/data/providerRegistry';
import { useOutcome } from '@/hooks/useOutcome';
import { genreName } from '@/data/mock/genres';
import { PageHeader } from '@/components/layout/PageHeader';
import { ContentGrid } from '@/components/content/ContentGrid';
import { LoadingState } from '@/components/common/LoadingState';
import { ErrorState, EmptyState } from '@/components/common/ErrorState';

export function GenreDetailPage() {
  const { genreId = '' } = useParams();
  const outcome = useOutcome(() => contentProvider.getByGenre(genreId), [genreId]);

  return (
    <>
      <PageHeader title={genreName(genreId)} />
      {outcome.status === 'loading' && <LoadingState />}
      {outcome.status === 'error' && <ErrorState message={outcome.message} />}
      {outcome.status === 'empty' && <EmptyState title="Nothing in this genre yet" />}
      {outcome.status === 'content' && <ContentGrid items={outcome.data} idPrefix={`genre-${genreId}-grid`} autoFocusFirst />}
    </>
  );
}
