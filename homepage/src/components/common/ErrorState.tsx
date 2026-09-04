import { Icon } from './Icon';
import styles from './ErrorState.module.css';

interface ErrorStateProps {
  title?: string;
  message?: string;
}

export function ErrorState({ title = 'Something went wrong', message }: ErrorStateProps) {
  return (
    <div className={styles.wrap}>
      <Icon name="error" className={styles.icon} />
      <div className={styles.title}>{title}</div>
      {message && <div className={styles.message}>{message}</div>}
    </div>
  );
}

export function EmptyState({ title, message }: { title: string; message?: string }) {
  return (
    <div className={styles.wrap}>
      <div className={styles.title}>{title}</div>
      {message && <div className={styles.message}>{message}</div>}
    </div>
  );
}
