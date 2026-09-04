import type { ReactNode } from 'react';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { TopNavigation } from './TopNavigation';
import { useSpatialNav } from '@/navigation/SpatialNavContext';
import styles from './AppShell.module.css';

export function AppShell({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const { pushBackHandler } = useSpatialNav();

  useEffect(() => {
    // Lowest-priority back handler: nothing else claimed Back, so leave the
    // current section the way Mango-TV's own rule describes — sections
    // replace each other, Back from Home does nothing further to catch.
    return pushBackHandler(() => {
      navigate(-1);
      return true;
    });
  }, [navigate, pushBackHandler]);

  return (
    <div className={styles.shell}>
      <TopNavigation />
      <main className={styles.content}>{children}</main>
    </div>
  );
}
