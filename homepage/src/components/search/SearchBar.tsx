import type { SearchFilter } from '@/types/content';
import { FocusContainer } from '@/components/common/FocusContainer';
import { Icon } from '@/components/common/Icon';
import styles from './SearchBar.module.css';

const KEY_ROWS = ['1234567890', 'QWERTYUIOP', 'ASDFGHJKL', 'ZXCVBNM'];

const FILTERS: { label: string; value: SearchFilter }[] = [
  { label: 'All', value: 'all' },
  { label: 'Movies', value: 'movie' },
  { label: 'TV Shows', value: 'series' },
];

interface SearchBarProps {
  query: string;
  onType: (char: string) => void;
  onBackspace: () => void;
  filter: SearchFilter;
  onFilterChange: (filter: SearchFilter) => void;
}

export function SearchBar({ query, onType, onBackspace, filter, onFilterChange }: SearchBarProps) {
  return (
    <div className={styles.wrap}>
      <div className={styles.fieldRow}>
        <div className={styles.field}>
          <Icon name="search" />
          <span className={styles.text}>
            {query || <span className={styles.placeholder}>Search movies and TV shows</span>}
            <span className={styles.cursor} />
          </span>
        </div>
      </div>

      <div className={styles.filters}>
        {FILTERS.map((f) => (
          <FocusContainer
            key={f.value}
            id={`search-filter-${f.value}`}
            className={styles.filterPill}
            data-active={filter === f.value}
            onSelect={() => onFilterChange(f.value)}
            autoFocus={f.value === 'all'}
          >
            {f.label}
          </FocusContainer>
        ))}
      </div>

      <div className={styles.keyboard}>
        {KEY_ROWS.flatMap((row) => row.split('')).map((char) => (
          <FocusContainer key={char} id={`search-key-${char}`} className={styles.key} onSelect={() => onType(char)}>
            {char}
          </FocusContainer>
        ))}
        <FocusContainer
          id="search-key-space"
          className={`${styles.key} ${styles.keyWide}`}
          onSelect={() => onType(' ')}
          aria-label="Space"
        >
          Space
        </FocusContainer>
        <FocusContainer
          id="search-key-backspace"
          className={`${styles.key} ${styles.keyWide}`}
          onSelect={onBackspace}
          aria-label="Backspace"
        >
          <Icon name="backspace" />
        </FocusContainer>
      </div>
    </div>
  );
}
