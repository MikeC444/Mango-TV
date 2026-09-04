import type { Genre } from '@/types/content';

export const GENRES: Genre[] = [
  { id: 'drama', name: 'Drama' },
  { id: 'action', name: 'Action' },
  { id: 'comedy', name: 'Comedy' },
  { id: 'thriller', name: 'Thriller' },
  { id: 'sci-fi', name: 'Sci-Fi' },
  { id: 'fantasy', name: 'Fantasy' },
  { id: 'horror', name: 'Horror' },
  { id: 'romance', name: 'Romance' },
  { id: 'crime', name: 'Crime' },
  { id: 'animation', name: 'Animation' },
  { id: 'documentary', name: 'Documentary' },
  { id: 'adventure', name: 'Adventure' },
];

export function genreName(id: string): string {
  return GENRES.find((g) => g.id === id)?.name ?? id;
}
