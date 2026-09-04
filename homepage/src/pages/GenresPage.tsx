import { useNavigate } from 'react-router-dom';
import { contentProvider } from '@/data/providerRegistry';
import { useOutcome } from '@/hooks/useOutcome';
import { PageHeader } from '@/components/layout/PageHeader';
import { FocusContainer } from '@/components/common/FocusContainer';
import { LoadingState } from '@/components/common/LoadingState';
import { ErrorState } from '@/components/common/ErrorState';
import { moodForGenre } from '@/data/mock/palette';
import styles from './GenresPage.module.css';

export function GenresPage() {
  const navigate = useNavigate();
  const outcome = useOutcome(() => contentProvider.getGenres(), []);

  return (
    <>
      <PageHeader title="Genres" />
      {outcome.status === 'loading' && <LoadingState />}
      {outcome.status === 'error' && <ErrorState message={outcome.message} />}
      {outcome.status === 'content' && (
        <div className={styles.grid}>
          {outcome.data.map((genre, i) => {
            const mood = moodForGenre(genre.id);
            return (
              <FocusContainer
                key={genre.id}
                id={`genre-tile-${genre.id}`}
                className={styles.tile}
                style={{ background: `linear-gradient(135deg, ${mood.from}, ${mood.to})` }}
                onSelect={() => navigate(`/genres/${genre.id}`)}
                autoFocus={i === 0}
              >
                {genre.name}
              </FocusContainer>
            );
          })}
        </div>
      )}
    </>
  );
}
