import { contentProvider } from '@/data/providerRegistry';
import { useOutcome } from '@/hooks/useOutcome';
import { PageHeader } from '@/components/layout/PageHeader';
import { ContentGrid } from '@/components/content/ContentGrid';
import { LoadingState } from '@/components/common/LoadingState';
import { ErrorState, EmptyState } from '@/components/common/ErrorState';

export function SeriesPage() {
  const outcome = useOutcome(() => contentProvider.getSeries(), []);

  return (
    <>
      <PageHeader title="TV Shows" />
      {outcome.status === 'loading' && <LoadingState />}
      {outcome.status === 'error' && <ErrorState message={outcome.message} />}
      {outcome.status === 'empty' && <EmptyState title="No TV shows available" />}
      {outcome.status === 'content' && <ContentGrid items={outcome.data} idPrefix="series-grid" autoFocusFirst />}
    </>
  );
}
