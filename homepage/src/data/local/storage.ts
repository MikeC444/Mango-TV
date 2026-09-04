/**
 * Local persistence for My List and playback progress — the same role
 * Mango-TV's DataStore-backed `data/local` package plays. Backed by
 * localStorage today; swap the implementation without touching callers.
 */

const MY_LIST_KEY = 'mango.myList.v1';
const PROGRESS_KEY = 'mango.progress.v1';

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function writeJson<T>(key: string, value: T): void {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage unavailable (private mode, quota) — fail silently, state just won't persist.
  }
}

const myListEvents = new EventTarget();

// useSyncExternalStore requires getSnapshot to return a stable reference
// when nothing changed — cache it, keyed on the raw stored string, instead
// of parsing a fresh array out of localStorage on every call.
let cachedMyListRaw: string | null | undefined;
let cachedMyListIds: string[] = [];

export function getMyListIds(): string[] {
  const raw = (() => {
    try {
      return localStorage.getItem(MY_LIST_KEY);
    } catch {
      return null;
    }
  })();
  if (raw !== cachedMyListRaw) {
    cachedMyListRaw = raw;
    cachedMyListIds = readJson<string[]>(MY_LIST_KEY, []);
  }
  return cachedMyListIds;
}

export function setMyListIds(ids: string[]): void {
  writeJson(MY_LIST_KEY, ids);
  myListEvents.dispatchEvent(new Event('change'));
}

export function subscribeMyList(listener: () => void): () => void {
  myListEvents.addEventListener('change', listener);
  return () => myListEvents.removeEventListener('change', listener);
}

export function getProgressMap(): Record<string, number> {
  return readJson<Record<string, number>>(PROGRESS_KEY, {});
}

export function setProgressMap(map: Record<string, number>): void {
  writeJson(PROGRESS_KEY, map);
}
