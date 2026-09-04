import { useEffect, useState } from 'react';
import type { Outcome } from '@/types/provider';

/** Runs an Outcome-returning fetch on mount / when deps change, tracking cancellation. */
export function useOutcome<T>(fetcher: () => Promise<Outcome<T>>, deps: unknown[]): Outcome<T> {
  const [outcome, setOutcome] = useState<Outcome<T>>({ status: 'loading' });

  useEffect(() => {
    let cancelled = false;
    setOutcome({ status: 'loading' });
    fetcher().then((result) => {
      if (!cancelled) setOutcome(result);
    });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return outcome;
}
