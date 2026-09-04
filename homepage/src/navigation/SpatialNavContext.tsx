import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';

export type Direction = 'up' | 'down' | 'left' | 'right';

interface FocusableEntry {
  id: string;
  el: HTMLElement;
  onSelect?: () => void;
  disabled?: boolean;
}

interface SpatialNavContextValue {
  focusedId: string | null;
  register: (entry: FocusableEntry) => void;
  unregister: (id: string) => void;
  updateOnSelect: (id: string, onSelect: (() => void) | undefined) => void;
  requestFocus: (id: string) => void;
  pushBackHandler: (handler: () => boolean) => () => void;
}

const SpatialNavContext = createContext<SpatialNavContextValue | null>(null);

const KEY_TO_DIRECTION: Record<string, Direction> = {
  ArrowUp: 'up',
  ArrowDown: 'down',
  ArrowLeft: 'left',
  ArrowRight: 'right',
};

const SELECT_KEYS = new Set(['Enter', ' ']);
const BACK_KEYS = new Set(['Escape', 'Backspace', 'BrowserBack', 'GoBack']);

function isTypingTarget(el: Element | null): boolean {
  if (!el) return false;
  const tag = el.tagName;
  return tag === 'INPUT' || tag === 'TEXTAREA' || (el as HTMLElement).isContentEditable;
}

/** Finds the best candidate to move focus to in `dir`, using center-point geometry. */
function findNearest(current: HTMLElement, dir: Direction, entries: FocusableEntry[]): FocusableEntry | null {
  const from = current.getBoundingClientRect();
  const fromCenter = { x: from.left + from.width / 2, y: from.top + from.height / 2 };

  let best: FocusableEntry | null = null;
  let bestScore = Infinity;

  for (const entry of entries) {
    if (entry.disabled || entry.el === current) continue;
    const rect = entry.el.getBoundingClientRect();
    if (rect.width === 0 && rect.height === 0) continue;
    const center = { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
    const dx = center.x - fromCenter.x;
    const dy = center.y - fromCenter.y;

    let primary: number;
    let cross: number;
    switch (dir) {
      case 'right':
        if (dx <= 4) continue;
        primary = dx;
        cross = dy;
        break;
      case 'left':
        if (dx >= -4) continue;
        primary = -dx;
        cross = dy;
        break;
      case 'down':
        if (dy <= 4) continue;
        primary = dy;
        cross = dx;
        break;
      case 'up':
        if (dy >= -4) continue;
        primary = -dy;
        cross = dx;
        break;
    }

    // Heavily penalize misalignment on the cross axis so navigation stays
    // predictable (moving down a column of cards doesn't jump sideways).
    const score = primary + Math.abs(cross) * 2.2;
    if (score < bestScore) {
      bestScore = score;
      best = entry;
    }
  }

  return best;
}

export function SpatialNavProvider({ children }: { children: ReactNode }) {
  const entriesRef = useRef<Map<string, FocusableEntry>>(new Map());
  const backHandlersRef = useRef<Array<() => boolean>>([]);
  const [focusedId, setFocusedId] = useState<string | null>(null);
  const focusedIdRef = useRef<string | null>(null);
  useEffect(() => {
    focusedIdRef.current = focusedId;
  }, [focusedId]);

  const register = useCallback((entry: FocusableEntry) => {
    entriesRef.current.set(entry.id, entry);
    // Functional update so this is correct even when several entries
    // register within the same commit — a plain ref read here would be
    // stale until this component's own effects flush (children's effects
    // run first), letting whichever entry mounted last silently steal the
    // default focus instead of the first one.
    setFocusedId((current) => (current === null ? entry.id : current));
  }, []);

  const unregister = useCallback((id: string) => {
    entriesRef.current.delete(id);
    setFocusedId((current) => {
      if (current !== id) return current;
      const next = entriesRef.current.values().next();
      return next.done ? null : next.value.id;
    });
  }, []);

  const updateOnSelect = useCallback((id: string, onSelect: (() => void) | undefined) => {
    const entry = entriesRef.current.get(id);
    if (entry) entry.onSelect = onSelect;
  }, []);

  const requestFocus = useCallback((id: string) => {
    if (entriesRef.current.has(id)) setFocusedId(id);
  }, []);

  const pushBackHandler = useCallback((handler: () => boolean) => {
    backHandlersRef.current.push(handler);
    return () => {
      backHandlersRef.current = backHandlersRef.current.filter((h) => h !== handler);
    };
  }, []);

  const move = useCallback((dir: Direction) => {
    const currentId = focusedIdRef.current;
    const entries = Array.from(entriesRef.current.values());
    if (!currentId) {
      if (entries[0]) setFocusedId(entries[0].id);
      return;
    }
    const currentEntry = entriesRef.current.get(currentId);
    if (!currentEntry) return;
    const next = findNearest(currentEntry.el, dir, entries);
    if (next) {
      setFocusedId(next.id);
      next.el.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
    }
  }, []);

  const activate = useCallback(() => {
    const currentId = focusedIdRef.current;
    if (!currentId) return;
    entriesRef.current.get(currentId)?.onSelect?.();
  }, []);

  const goBack = useCallback(() => {
    for (let i = backHandlersRef.current.length - 1; i >= 0; i--) {
      const handled = backHandlersRef.current[i]();
      if (handled) return;
    }
  }, []);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const typing = isTypingTarget(document.activeElement);
      const dir = KEY_TO_DIRECTION[e.key];
      if (dir && !typing) {
        e.preventDefault();
        move(dir);
        return;
      }
      if (SELECT_KEYS.has(e.key) && !typing) {
        e.preventDefault();
        activate();
        return;
      }
      if (BACK_KEYS.has(e.key)) {
        e.preventDefault();
        goBack();
      }
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [move, activate, goBack]);

  const value = useMemo(
    () => ({ focusedId, register, unregister, updateOnSelect, requestFocus, pushBackHandler }),
    [focusedId, register, unregister, updateOnSelect, requestFocus, pushBackHandler],
  );

  return <SpatialNavContext.Provider value={value}>{children}</SpatialNavContext.Provider>;
}

export function useSpatialNav(): SpatialNavContextValue {
  const ctx = useContext(SpatialNavContext);
  if (!ctx) throw new Error('useSpatialNav must be used within a SpatialNavProvider');
  return ctx;
}
