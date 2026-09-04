import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ContentSummary } from '@/types/content';
import { useFocusable } from '@/navigation/useFocusable';
import { Artwork } from '@/components/common/Artwork';
import { Icon } from '@/components/common/Icon';
import { ProgressBar } from './ProgressBar';
import styles from './ContentCard.module.css';

export interface ContentCardProps {
  item: ContentSummary;
  id: string;
  /** Continue Watching cards show a resume progress bar and use a 16:9 still instead of a poster. */
  variant?: 'standard' | 'continue-watching';
  size?: 'default' | 'large';
  autoFocus?: boolean;
  onFocusVisible?: () => void;
}

export function ContentCard({ item, id, variant = 'standard', size = 'default', autoFocus, onFocusVisible }: ContentCardProps) {
  const navigate = useNavigate();
  const isContinueWatching = variant === 'continue-watching';
  const { ref, focused } = useFocusable(id, {
    onSelect: () => navigate(isContinueWatching ? `/title/${item.id}?play=1` : `/title/${item.id}`),
    autoFocus,
  });

  useEffect(() => {
    if (focused) onFocusVisible?.();
  }, [focused, onFocusVisible]);

  const artwork = isContinueWatching ? item.backdrop : item.poster;
  const subtitle = isContinueWatching
    ? item.resumeEpisode
      ? `S${item.resumeEpisode.seasonNumber} E${item.resumeEpisode.episodeNumber} · ${item.resumeEpisode.title}`
      : 'Resume'
    : `${item.year} · ${item.genres[0] ?? ''}`;

  return (
    <button
      ref={(node) => {
        ref.current = node;
      }}
      type="button"
      className={styles.card}
      data-focused={focused || undefined}
      data-large={size === 'large' || undefined}
      data-backdrop={isContinueWatching || undefined}
      onClick={() => navigate(isContinueWatching ? `/title/${item.id}?play=1` : `/title/${item.id}`)}
    >
      <div className={styles.poster}>
        <Artwork
          artwork={artwork}
          size={isContinueWatching ? 'backdrop-md' : 'poster-md'}
          genre={item.genres[0]}
          title={item.title}
          showLabel={!isContinueWatching}
          alt={item.title}
        />
        <div className={styles.playHint} aria-hidden="true">
          <div className={styles.playHintCircle}>
            <Icon name="play" />
          </div>
        </div>
        {item.type === 'series' && !isContinueWatching && <span className={styles.badge}>Series</span>}
        {isContinueWatching && (
          <div className={styles.progressWrap}>
            <ProgressBar progress={item.progress ?? 0} />
          </div>
        )}
      </div>
      <div className={styles.meta}>
        <div className={styles.title}>{item.title}</div>
        {isContinueWatching ? (
          <div className={styles.resumeLabel}>{subtitle}</div>
        ) : (
          <div className={styles.subMeta}>{subtitle}</div>
        )}
      </div>
    </button>
  );
}

export function MovieCard(props: Omit<ContentCardProps, 'variant'>) {
  return <ContentCard {...props} variant="standard" />;
}

export function SeriesCard(props: Omit<ContentCardProps, 'variant'>) {
  return <ContentCard {...props} variant="standard" />;
}

export function ContinueWatchingCard(props: Omit<ContentCardProps, 'variant'>) {
  return <ContentCard {...props} variant="continue-watching" />;
}
