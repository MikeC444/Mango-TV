import { useEffect, useRef } from 'react';
import { useSpatialNav } from './SpatialNavContext';

interface UseFocusableOptions {
  onSelect?: () => void;
  disabled?: boolean;
  /** If true and nothing is focused yet, this becomes the initial focus target. */
  autoFocus?: boolean;
}

/**
 * Registers an element as a D-pad focus target. Returns a ref to attach to
 * the focusable DOM node and whether it currently holds focus — drive all
 * visual focus state (scale, glow, elevation) off `focused`, never off CSS
 * `:hover` or `:focus`, since the remote never hovers.
 */
export function useFocusable(id: string, options: UseFocusableOptions = {}) {
  const { onSelect, disabled, autoFocus } = options;
  const { focusedId, register, unregister, updateOnSelect, requestFocus } = useSpatialNav();
  const ref = useRef<HTMLElement | null>(null);
  const onSelectRef = useRef(onSelect);
  onSelectRef.current = onSelect;

  useEffect(() => {
    if (!ref.current) return;
    register({ id, el: ref.current, onSelect: () => onSelectRef.current?.(), disabled });
    if (autoFocus) requestFocus(id);
    return () => unregister(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, disabled]);

  useEffect(() => {
    updateOnSelect(id, onSelect);
  }, [id, onSelect, updateOnSelect]);

  return { ref, focused: focusedId === id };
}
