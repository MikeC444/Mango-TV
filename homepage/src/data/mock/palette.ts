/**
 * Procedural "artwork" for mock content. There are no real posters or
 * stills bundled — instead each title gets a deterministic, cinematic
 * gradient derived from its genre and id, rendered by <Artwork>. This keeps
 * the mock layer dependency-free (no binary assets, no network fetches) and
 * swaps out cleanly: once a real ContentProvider resolves ArtworkRef to an
 * actual image URL, <Artwork> renders that instead automatically.
 */

export interface Mood {
  from: string;
  via: string;
  to: string;
}

const MOODS: Record<string, Mood> = {
  drama: { from: '#37497a', via: '#4a3f72', to: '#0a0908' },
  action: { from: '#a4471f', via: '#7a2417', to: '#0a0908' },
  comedy: { from: '#a6791c', via: '#8a4c14', to: '#0a0908' },
  thriller: { from: '#146d6a', via: '#1c4a52', to: '#0a0908' },
  'sci-fi': { from: '#2f3fa0', via: '#4b2f8c', to: '#0a0908' },
  fantasy: { from: '#22794f', via: '#3b3690', to: '#0a0908' },
  horror: { from: '#7a1622', via: '#4a121e', to: '#0a0908' },
  romance: { from: '#8c2447', via: '#6c1e3a', to: '#0a0908' },
  crime: { from: '#3a3a4c', via: '#552626', to: '#0a0908' },
  animation: { from: '#4632a8', via: '#166f6a', to: '#0a0908' },
  documentary: { from: '#4a453a', via: '#5c5344', to: '#0a0908' },
  adventure: { from: '#2f6d33', via: '#5c7a1c', to: '#0a0908' },
};

const FALLBACK: Mood = { from: '#3a3a38', via: '#4a463d', to: '#0a0908' };

export function moodForGenre(genre: string): Mood {
  return MOODS[genre] ?? FALLBACK;
}

/** Small deterministic hash so the same key always yields the same angle/accent. */
export function hashKey(key: string): number {
  let h = 0;
  for (let i = 0; i < key.length; i++) {
    h = (h * 31 + key.charCodeAt(i)) >>> 0;
  }
  return h;
}
