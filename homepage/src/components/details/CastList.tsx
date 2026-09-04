import type { Person } from '@/types/content';
import styles from './CastList.module.css';

export function CastList({ cast }: { cast: Person[] }) {
  if (cast.length === 0) return null;
  return (
    <div className={styles.list}>
      {cast.map((person) => (
        <div className={styles.person} key={person.id}>
          <div className={styles.name}>{person.name}</div>
          {person.role && <div className={styles.role}>{person.role}</div>}
        </div>
      ))}
    </div>
  );
}
