import type { Season } from '@/types/content';
import { FocusContainer } from '@/components/common/FocusContainer';
import styles from './SeasonSelector.module.css';

interface SeasonSelectorProps {
  seasons: Season[];
  activeSeason: number;
  onChange: (seasonNumber: number) => void;
  idPrefix: string;
}

export function SeasonSelector({ seasons, activeSeason, onChange, idPrefix }: SeasonSelectorProps) {
  if (seasons.length <= 1) return null;
  return (
    <div className={styles.wrap}>
      {seasons.map((season) => (
        <FocusContainer
          key={season.seasonNumber}
          id={`${idPrefix}-season-${season.seasonNumber}`}
          className={styles.pill}
          data-active={season.seasonNumber === activeSeason}
          onSelect={() => onChange(season.seasonNumber)}
        >
          {season.title}
        </FocusContainer>
      ))}
    </div>
  );
}
