import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { FocusContainer } from '@/components/common/FocusContainer';
import { Icon } from '@/components/common/Icon';
import styles from './TopNavigation.module.css';

const LINKS: { label: string; path: string }[] = [
  { label: 'Home', path: '/' },
  { label: 'Movies', path: '/movies' },
  { label: 'TV Shows', path: '/series' },
  { label: 'Genres', path: '/genres' },
  { label: 'My List', path: '/my-list' },
];

export function TopNavigation() {
  const navigate = useNavigate();
  const location = useLocation();
  const [solid, setSolid] = useState(false);

  useEffect(() => {
    const onScroll = () => setSolid(window.scrollY > 12);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <nav className={styles.nav} data-solid={solid}>
      <div className={styles.left}>
        <div className={styles.logo}>
          <span className={styles.logoMark} aria-hidden="true" />
          Mango
        </div>
        <div className={styles.links}>
          {LINKS.map((link) => (
            <FocusContainer
              key={link.path}
              id={`nav-${link.path}`}
              className={styles.link}
              focusedClassName={styles.link}
              data-active={location.pathname === link.path}
              onSelect={() => navigate(link.path)}
            >
              {link.label}
            </FocusContainer>
          ))}
        </div>
      </div>
      <div className={styles.right}>
        <FocusContainer
          id="nav-search"
          className={styles.iconButton}
          onSelect={() => navigate('/search')}
          aria-label="Search"
        >
          <Icon name="search" />
        </FocusContainer>
        <FocusContainer
          id="nav-settings"
          className={styles.iconButton}
          onSelect={() => navigate('/settings')}
          aria-label="Settings"
        >
          <Icon name="settings" />
        </FocusContainer>
        <FocusContainer
          id="nav-profile"
          className={styles.avatar}
          onSelect={() => navigate('/settings/account')}
          aria-label="Profile"
        >
          M
        </FocusContainer>
      </div>
    </nav>
  );
}
