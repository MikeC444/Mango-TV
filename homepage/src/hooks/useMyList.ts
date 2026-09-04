import { useCallback, useSyncExternalStore } from 'react';
import { getMyListIds, subscribeMyList } from '@/data/local/storage';
import { contentProvider } from '@/data/providerRegistry';

/** Reactive My List membership, backed by local storage — updates instantly across every mounted card. */
export function useMyListIds(): string[] {
  return useSyncExternalStore(subscribeMyList, getMyListIds, () => []);
}

export function useIsInMyList(id: string): boolean {
  const ids = useMyListIds();
  return ids.includes(id);
}

export function useToggleMyList(id: string) {
  const inList = useIsInMyList(id);
  return useCallback(async () => {
    if (inList) await contentProvider.removeFromMyList(id);
    else await contentProvider.addToMyList(id);
  }, [id, inList]);
}
