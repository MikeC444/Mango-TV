import type { Episode } from '@/types/content';
import { Artwork } from '@/components/common/Artwork';
import { Icon } from '@/components/common/Icon';
import { ProgressBar } from '@/components/content/ProgressBar';
import { useFocusable } from '@/navigation/useFocusable';
import styles from './EpisodeList.module.css';

function EpisodeRow({ episode, onSelect, idPrefix }: { episode: Episode; onSelect: () => void; idPrefix: string }) {
  const { ref, focused } = useFocusable(`${idPrefix}-ep-${episode.id}`, { onSelect });

  return (
    <button
      ref={(node) => {
        ref.current = node;
      }}
      type="button"
      className={styles.episode}
      data-focused={focused || undefined}
      onClick={onSelect}
    >
      <div className={styles.thumbWrap}>
        <Artwork artwork={episode.still} size="backdrop-md" alt={episode.title} />
        <div className={styles.playHint} aria-hidden="true">
          <Icon name="play" />
        </div>
        {episode.progress !== undefined && episode.progress > 0 && (
          <div className={styles.progressWrap}>
            <ProgressBar progress={episode.progress} />
          </div>
        )}
      </div>
      <div className={styles.body}>
        <div className={styles.headRow}>
          <span className={styles.epNumber}>{episode.episodeNumber}</span>
          <span className={styles.epTitle}>{episode.title}</span>
          <span className={styles.runtime}>{episode.runtimeMinutes}m</span>
        </div>
        <p className={styles.synopsis}>{episode.synopsis}</p>
      </div>
    </button>
  );
}

export function EpisodeList({
  episodes,
  idPrefix,
  onSelectEpisode,
}: {
  episodes: Episode[];
  idPrefix: string;
  onSelectEpisode: (episode: Episode) => void;
}) {
  return (
    <div className={styles.list}>
      {episodes.map((episode) => (
        <EpisodeRow key={episode.id} episode={episode} idPrefix={idPrefix} onSelect={() => onSelectEpisode(episode)} />
      ))}
    </div>
  );
}
