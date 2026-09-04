import type { ArtworkRef } from '@/types/content';

export type ArtworkSize = 'poster-sm' | 'poster-md' | 'backdrop-md' | 'backdrop-lg';

/**
 * Resolves an ArtworkRef + target size into a fetchable URL. The mock
 * provider has no real images, so this always returns null and callers
 * (see components/common/Artwork.tsx) fall back to a generated gradient —
 * exactly the seam described in Mango-TV's cache/ArtworkSource.kt: a real
 * backend returns a size-appropriate URL here and nothing above this
 * function changes.
 */
export function resolveArtworkUrl(_ref: ArtworkRef, _size: ArtworkSize): string | null {
  return null;
}
