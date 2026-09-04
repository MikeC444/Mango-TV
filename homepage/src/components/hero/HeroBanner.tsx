import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ContentSummary } from '@/types/content';
import { Artwork } from '@/components/common/Artwork';
import { FocusContainer } from '@/components/common/FocusContainer';
import { Icon } from '@/components/common/Icon';
import { useIsInMyList, useToggleMyList } from '@/hooks/useMyList';
import styles from './HeroBanner.module.css';

const ROTATE_MS = 9000;

function HeroActions({ item }: { item: ContentSummary }) {
  const navigate = useNavigate();
  const inList = useIsInMyList(item.id);
  const toggle = useToggleMyList(item.id);

  return (
    <div className={styles.actions}>
      <FocusContainer
        id={`hero-play-${item.id}`}
        className={styles.playButton}
        onSelect={() => navigate(`/title/${item.id}?play=1`)}
        autoFocus
      >
        <Icon name="play" /> Play
      </FocusContainer>
      <FocusContainer id={`hero-list-${item.id}`} className={styles.secondaryButton} onSelect={toggle}>
        <Icon name={inList ? 'check' : 'plus'} /> {inList ? 'In My List' : 'My List'}
      </FocusContainer>
      <FocusContainer
        id={`hero-info-${item.id}`}
        className={styles.secondaryButton}
        onSelect={() => navigate(`/title/${item.id}`)}
      >
        <Icon name="info" /> More Info
      </FocusContainer>
    </div>
  );
}

export function HeroBanner({ items }: { items: ContentSummary[] }) {
  const [index, setIndex] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (items.length <= 1) return;
    timerRef.current = setInterval(() => {
      setIndex((i) => (i + 1) % items.length);
    }, ROTATE_MS);
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [items.length]);

  if (items.length === 0) return null;
  const item = items[index];

  return (
    <section className={styles.hero} aria-label={`Featured: ${item.title}`}>
      <div className={styles.backdrop}>
        {items.map((it, i) => (
          <div key={it.id} className={styles.backdropLayer} data-active={i === index}>
            <Artwork artwork={it.backdrop} size="backdrop-lg" genre={it.genres[0]} alt="" eager={i === 0} />
          </div>
        ))}
        <div className={styles.scrim} />
      </div>

      <div className={styles.content}>
        <div className={styles.badge}>Featured on Mango</div>
        <h1 className={styles.title}>{item.title}</h1>
        <div className={styles.meta}>
          <span className={styles.rating}>
            <Icon name="star" /> {item.rating.toFixed(1)}
          </span>
          <span className={styles.metaDot} />
          <span>{item.year}</span>
          <span className={styles.metaDot} />
          <span className={styles.cert}>{item.certification}</span>
          <span className={styles.metaDot} />
          <span>{item.type === 'movie' ? formatRuntime(item.runtimeMinutes) : `${item.seasonCount} Season${item.seasonCount === 1 ? '' : 's'}`}</span>
          <span className={styles.metaDot} />
          <span>{item.genres.slice(0, 3).join(' · ')}</span>
        </div>
        <p className={styles.synopsis}>{item.synopsis}</p>
        <HeroActions item={item} />
      </div>

      {items.length > 1 && (
        <div className={styles.dots}>
          {items.map((it, i) => (
            <FocusContainer
              key={it.id}
              id={`hero-dot-${it.id}`}
              className={styles.dot}
              data-active={i === index}
              onSelect={() => setIndex(i)}
              aria-label={`Show ${it.title}`}
            />
          ))}
        </div>
      )}
    </section>
  );
}

function formatRuntime(minutes?: number): string {
  if (!minutes) return '';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}
