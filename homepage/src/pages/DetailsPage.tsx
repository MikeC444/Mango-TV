import { useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { contentProvider } from '@/data/providerRegistry';
import { useOutcome } from '@/hooks/useOutcome';
import { useIsInMyList, useToggleMyList } from '@/hooks/useMyList';
import { Artwork } from '@/components/common/Artwork';
import { Icon } from '@/components/common/Icon';
import { FocusContainer } from '@/components/common/FocusContainer';
import { Modal } from '@/components/common/Modal';
import { LoadingState } from '@/components/common/LoadingState';
import { ErrorState } from '@/components/common/ErrorState';
import { CastList } from '@/components/details/CastList';
import { SeasonSelector } from '@/components/details/SeasonSelector';
import { EpisodeList } from '@/components/details/EpisodeList';
import type { Episode } from '@/types/content';
import styles from './DetailsPage.module.css';

function formatRuntime(minutes?: number): string {
  if (!minutes) return '';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

export function DetailsPage() {
  const { id = '' } = useParams();
  const [searchParams] = useSearchParams();
  const outcome = useOutcome(() => contentProvider.getDetail(id), [id]);
  const inList = useIsInMyList(id);
  const toggleList = useToggleMyList(id);
  const [activeSeason, setActiveSeason] = useState(1);
  const [nowPlaying, setNowPlaying] = useState<Episode | null>(null);
  const [showTrailer, setShowTrailer] = useState(false);
  const [playing, setPlaying] = useState(searchParams.get('play') === '1');

  const detail = outcome.status === 'content' ? outcome.data : null;

  const season = useMemo(
    () => detail?.seasons?.find((s) => s.seasonNumber === activeSeason) ?? detail?.seasons?.[0],
    [detail, activeSeason],
  );

  if (outcome.status === 'loading') return <LoadingState />;
  if (outcome.status === 'error') return <ErrorState message={outcome.message} />;
  if (!detail) return <ErrorState title="Title not found" />;

  return (
    <>
      <div className={styles.backdrop}>
        <Artwork artwork={detail.backdrop} size="backdrop-lg" genre={detail.genres[0]} alt="" eager />
        <div className={styles.scrim} />
      </div>

      <div className={styles.body}>
        <div className={styles.posterWrap}>
          <Artwork artwork={detail.poster} size="poster-md" genre={detail.genres[0]} title={detail.title} showLabel alt={detail.title} />
        </div>

        <div className={styles.info}>
          <h1 className={styles.title}>{detail.title}</h1>
          <div className={styles.meta}>
            <span className={styles.rating}>
              <Icon name="star" /> {detail.rating.toFixed(1)}
            </span>
            <span className={styles.metaDot} />
            <span>{detail.year}</span>
            <span className={styles.metaDot} />
            <span className={styles.cert}>{detail.certification}</span>
            <span className={styles.metaDot} />
            <span>
              {detail.type === 'movie'
                ? formatRuntime(detail.runtimeMinutes)
                : `${detail.seasonCount} Season${detail.seasonCount === 1 ? '' : 's'}`}
            </span>
          </div>
          <div className={styles.genres}>{detail.genres.join(' · ')}</div>
          <p className={styles.synopsis}>{detail.synopsis}</p>

          <div className={styles.actions}>
            <FocusContainer id="detail-play" className={styles.playButton} onSelect={() => setPlaying(true)} autoFocus>
              <Icon name="play" /> {detail.progress ? 'Resume' : 'Play'}
            </FocusContainer>
            <FocusContainer id="detail-mylist" className={styles.iconButton} onSelect={toggleList} aria-label="Toggle My List">
              <Icon name={inList ? 'check' : 'plus'} />
            </FocusContainer>
            {detail.trailerAvailable && (
              <FocusContainer id="detail-trailer" className={styles.iconButton} onSelect={() => setShowTrailer(true)} aria-label="Trailer">
                <Icon name="info" />
              </FocusContainer>
            )}
          </div>

          {(detail.director || (detail.creators && detail.creators.length > 0)) && (
            <div className={styles.crewRow}>
              {detail.director && (
                <div>
                  <strong>Director </strong>
                  {detail.director.name}
                </div>
              )}
              {detail.creators && detail.creators.length > 0 && (
                <div>
                  <strong>Creator </strong>
                  {detail.creators.map((c) => c.name).join(', ')}
                </div>
              )}
            </div>
          )}

          {detail.cast.length > 0 && (
            <div className={styles.section}>
              <div className={styles.sectionTitle}>Cast</div>
              <CastList cast={detail.cast} />
            </div>
          )}

          {detail.seasons && season && (
            <div className={styles.section}>
              <div className={styles.sectionTitle}>Episodes</div>
              <SeasonSelector
                seasons={detail.seasons}
                activeSeason={activeSeason}
                onChange={setActiveSeason}
                idPrefix="detail"
              />
              <div style={{ marginTop: 16 }}>
                <EpisodeList episodes={season.episodes} idPrefix="detail" onSelectEpisode={setNowPlaying} />
              </div>
            </div>
          )}
        </div>
      </div>

      {playing && (
        <Modal title={`Now Playing — ${detail.title}`} onClose={() => setPlaying(false)}>
          <p className={styles.modalText}>
            Mango-Homepage is a UI-only frontend: this is the point where playback would hand off to a
            PlaybackSource resolved by the active ContentProvider. No video decoder is wired up in this
            mock build.
          </p>
        </Modal>
      )}

      {nowPlaying && (
        <Modal title={nowPlaying.title} onClose={() => setNowPlaying(null)}>
          <p className={styles.modalText}>{nowPlaying.synopsis}</p>
        </Modal>
      )}

      {showTrailer && (
        <Modal title={`Trailer — ${detail.title}`} onClose={() => setShowTrailer(false)}>
          <p className={styles.modalText}>Trailer playback isn't wired up in this mock build yet.</p>
        </Modal>
      )}
    </>
  );
}
