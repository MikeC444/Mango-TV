import { contentProvider } from '@/data/providerRegistry';
import { useOutcome } from '@/hooks/useOutcome';
import { useMyListIds } from '@/hooks/useMyList';
import { PageHeader } from '@/components/layout/PageHeader';
import { ContentGrid } from '@/components/content/ContentGrid';
import { LoadingState } from '@/components/common/LoadingState';
import { ErrorState, EmptyState } from '@/components/common/ErrorState';

export function MyListPage() {
  const myListIds = useMyListIds();
  const outcome = useOutcome(() => contentProvider.getMyList(), [myListIds.join(',')]);

  return (
    <>
      <PageHeader title="My List" subtitle="Movies and shows you've saved to watch later" />
      {outcome.status === 'loading' && <LoadingState />}
      {outcome.status === 'error' && <ErrorState message={outcome.message} />}
      {outcome.status === 'empty' && (
        <EmptyState title="Your list is empty" message="Add movies and shows from their detail page to see them here." />
      )}
      {outcome.status === 'content' && <ContentGrid items={outcome.data} idPrefix="my-list-grid" autoFocusFirst />}
    </>
  );
}
