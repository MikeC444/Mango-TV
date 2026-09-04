import { useEffect, useState } from 'react';
import type { SearchFilter } from '@/types/content';
import { contentProvider } from '@/data/providerRegistry';
import { useOutcome } from '@/hooks/useOutcome';
import { SearchBar } from '@/components/search/SearchBar';
import { ContentCard } from '@/components/content/ContentCard';
import { LoadingState } from '@/components/common/LoadingState';
import { ErrorState, EmptyState } from '@/components/common/ErrorState';
import styles from './SearchPage.module.css';

export function SearchPage() {
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<SearchFilter>('all');
  const outcome = useOutcome(() => contentProvider.search(query, filter), [query, filter]);

  // Physical keyboard support for desktop/dev use, on top of the on-screen keyboard the D-pad drives.
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (document.activeElement?.tagName === 'INPUT' || document.activeElement?.tagName === 'TEXTAREA') return;
      if (e.key === 'Backspace') {
        setQuery((q) => q.slice(0, -1));
      } else if (e.key.length === 1 && /[a-zA-Z0-9 ]/.test(e.key)) {
        setQuery((q) => (q + e.key).slice(0, 40));
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  return (
    <div className={styles.layout}>
      <SearchBar
        query={query}
        onType={(char) => setQuery((q) => (q + char).slice(0, 40))}
        onBackspace={() => setQuery((q) => q.slice(0, -1))}
        filter={filter}
        onFilterChange={setFilter}
      />
      <div className={styles.results}>
        {query.trim() === '' && <p className={styles.hint}>Type a title, genre, or keyword to search.</p>}
        {outcome.status === 'loading' && query.trim() !== '' && <LoadingState compact />}
        {outcome.status === 'error' && <ErrorState message={outcome.message} />}
        {outcome.status === 'empty' && query.trim() !== '' && (
          <EmptyState title={`No results for "${query}"`} message="Try a different title or genre." />
        )}
        {outcome.status === 'content' && (
          <div className={styles.resultsGrid}>
            {outcome.data.map((item) => (
              <ContentCard key={item.id} id={`search-result-${item.id}`} item={item} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
