import { useMemo } from 'react';
import type { ArtworkRef } from '@/types/content';
import { resolveArtworkUrl, type ArtworkSize } from '@/data/artwork';
import { hashKey, moodForGenre } from '@/data/mock/palette';
import styles from './Artwork.module.css';

interface ArtworkProps {
  artwork: ArtworkRef;
  size: ArtworkSize;
  genre?: string;
  title?: string;
  /** Show the title monogram overlay (used for poster-shaped cards, not backdrops). */
  showLabel?: boolean;
  alt: string;
  eager?: boolean;
  className?: string;
}

/**
 * Renders real artwork when a provider resolves a URL, otherwise a
 * deterministic cinematic gradient derived from the title's genre and id —
 * see data/artwork.ts and data/mock/palette.ts for the resolution seam.
 */
export function Artwork({ artwork, size, genre, title, showLabel, alt, eager, className }: ArtworkProps) {
  const url = resolveArtworkUrl(artwork, size);
  const mood = useMemo(() => moodForGenre(genre ?? ''), [genre]);
  const hash = useMemo(() => hashKey(artwork.key), [artwork.key]);
  const angle = 100 + (hash % 60);
  const glowX = 15 + (hash % 40);
  const glowY = 10 + ((hash >> 4) % 35);

  if (url) {
    return (
      <div className={`${styles.wrap} ${className ?? ''}`}>
        <img
          className={styles.img}
          src={url}
          alt={alt}
          loading={eager ? 'eager' : 'lazy'}
          decoding="async"
        />
      </div>
    );
  }

  return (
    <div
      className={`${styles.wrap} ${className ?? ''}`}
      role="img"
      aria-label={alt}
      style={{
        backgroundImage: `radial-gradient(130% 110% at ${glowX}% ${glowY}%, ${mood.via} 0%, transparent 58%), radial-gradient(120% 100% at ${100 - glowX}% ${100 - glowY}%, ${mood.from} 0%, transparent 62%), linear-gradient(${angle}deg, ${mood.from}, ${mood.to})`,
        backgroundColor: mood.to,
      }}
    >
      <div className={styles.grain} aria-hidden="true" />
      <div className={styles.vignette} aria-hidden="true" />
      {showLabel && title && (
        <div className={styles.monogram} aria-hidden="true">
          <span className={styles.monogramText}>{title}</span>
        </div>
      )}
    </div>
  );
}
