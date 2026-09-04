import type { ContentProvider } from '@/types/provider';
import { MockContentProvider } from './mockProvider';

/**
 * The single place the app decides which ContentProvider is active.
 *
 * Today this is the bundled mock library. Pointing Mango-Homepage at a real
 * backend or a Stremio-style add-on later means implementing
 * ContentProvider elsewhere and changing the line below — every screen
 * imports `contentProvider` from here and never constructs a provider
 * itself, so nothing else changes.
 */
export const contentProvider: ContentProvider = new MockContentProvider();
