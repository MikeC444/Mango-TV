import styles from './LoadingState.module.css';

export function LoadingState({ compact }: { compact?: boolean }) {
  return (
    <div className={`${styles.wrap} ${compact ? styles.row : ''}`} role="status" aria-label="Loading">
      <div className={styles.spinner} />
    </div>
  );
}
