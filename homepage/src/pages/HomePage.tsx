import { useCallback } from 'react';
import { contentProvider } from '@/data/providerRegistry';
import { useOutcome } from '@/hooks/useOutcome';
import { useMyListIds } from '@/hooks/useMyList';
import { HeroBanner } from '@/components/hero/HeroBanner';
import { ContentRow } from '@/components/content/ContentRow';
import { LoadingState } from '@/components/common/LoadingState';
import { ErrorState } from '@/components/common/ErrorState';
import styles from './HomePage.module.css';

export function HomePage() {
  // Re-fetch home rows when My List / progress changes so Continue Watching stays live.
  const myListIds = useMyListIds();
  const heroOutcome = useOutcome(() => contentProvider.getFeaturedForHero(), []);
  const rowsOutcome = useOutcome(() => contentProvider.getHomeRows(), [myListIds.length]);

  const heroItems = heroOutcome.status === 'content' ? heroOutcome.data : [];

  const renderRows = useCallback(() => {
    if (rowsOutcome.status === 'loading') return <LoadingState />;
    if (rowsOutcome.status === 'error') return <ErrorState message={rowsOutcome.message} />;
    if (rowsOutcome.status === 'empty') return <ErrorState title="Nothing to show yet" />;
    return rowsOutcome.data.map((row) => <ContentRow key={row.id} row={row} />);
  }, [rowsOutcome]);

  return (
    <>
      {heroOutcome.status === 'content' && <HeroBanner items={heroItems} />}
      <div className={styles.rows}>{renderRows()}</div>
    </>
  );
}
