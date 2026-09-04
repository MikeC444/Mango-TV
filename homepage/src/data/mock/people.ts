import type { Person } from '@/types/content';

/** A reusable pool of fictional cast/crew — combined per-title in titles.ts. */
export const PEOPLE_POOL: Person[] = [
  { id: 'p01', name: 'Elena Marsh' },
  { id: 'p02', name: 'Theo Okafor' },
  { id: 'p03', name: 'Ingrid Solberg' },
  { id: 'p04', name: 'Marcus Webb' },
  { id: 'p05', name: 'Priya Anand' },
  { id: 'p06', name: 'Diego Fuentes' },
  { id: 'p07', name: 'Naomi Cross' },
  { id: 'p08', name: 'Hiro Tanaka' },
  { id: 'p09', name: 'Freya Lindqvist' },
  { id: 'p10', name: 'Samuel Iyer' },
  { id: 'p11', name: 'Clara Duval' },
  { id: 'p12', name: 'Owen Blackwood' },
  { id: 'p13', name: 'Amara Nwosu' },
  { id: 'p14', name: 'Felix Draven' },
  { id: 'p15', name: 'Yuki Sato' },
  { id: 'p16', name: 'Rosa Herrera' },
  { id: 'p17', name: 'Callum Reyes' },
  { id: 'p18', name: 'Junot Delacroix' },
  { id: 'p19', name: 'Sienna Frost' },
  { id: 'p20', name: 'Malik Preston' },
  { id: 'p21', name: 'Vera Kaminski' },
  { id: 'p22', name: 'Adrian Voss' },
  { id: 'p23', name: 'Talia Brandt' },
  { id: 'p24', name: 'Noor Haddad' },
  { id: 'p25', name: 'Miles Ashworth' },
];

export function personById(id: string): Person {
  const found = PEOPLE_POOL.find((p) => p.id === id);
  if (!found) throw new Error(`Unknown person id: ${id}`);
  return found;
}

export function castFor(ids: string[], roles: string[]): Person[] {
  return ids.map((id, i) => ({ ...personById(id), role: roles[i] }));
}
