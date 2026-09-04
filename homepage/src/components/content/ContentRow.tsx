import { useRef } from 'react';
import type { ContentRow as ContentRowData } from '@/types/content';
import { ContentCard } from './ContentCard';
import styles from './ContentRow.module.css';

export function ContentRow({ row }: { row: ContentRowData }) {
  const trackRef = useRef<HTMLDivElement>(null);

  return (
    <section className={styles.row} aria-label={row.title}>
      <h2 className={styles.title}>{row.title}</h2>
      <div className={styles.edge + ' ' + styles.edgeLeft} aria-hidden="true" />
      <div className={styles.track} ref={trackRef}>
        {row.items.map((item) => (
          <ContentCard
            key={item.id}
            id={`card-${row.id}-${item.id}`}
            item={item}
            variant={row.variant === 'continue-watching' ? 'continue-watching' : 'standard'}
          />
        ))}
      </div>
      <div className={styles.edge + ' ' + styles.edgeRight} aria-hidden="true" />
    </section>
  );
}
