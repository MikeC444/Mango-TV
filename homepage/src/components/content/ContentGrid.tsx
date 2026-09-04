import type { ContentSummary } from '@/types/content';
import { ContentCard } from './ContentCard';
import styles from './ContentGrid.module.css';

export function ContentGrid({
  items,
  idPrefix,
  autoFocusFirst,
}: {
  items: ContentSummary[];
  idPrefix: string;
  autoFocusFirst?: boolean;
}) {
  return (
    <div className={styles.grid}>
      {items.map((item, i) => (
        <ContentCard key={item.id} id={`${idPrefix}-${item.id}`} item={item} autoFocus={autoFocusFirst && i === 0} />
      ))}
    </div>
  );
}
