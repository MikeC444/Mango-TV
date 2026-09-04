import { Route, Routes, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { AppShell } from '@/components/layout/AppShell';
import { HomePage } from '@/pages/HomePage';
import { MoviesPage } from '@/pages/MoviesPage';
import { SeriesPage } from '@/pages/SeriesPage';
import { GenresPage } from '@/pages/GenresPage';
import { GenreDetailPage } from '@/pages/GenreDetailPage';
import { MyListPage } from '@/pages/MyListPage';
import { SearchPage } from '@/pages/SearchPage';
import { DetailsPage } from '@/pages/DetailsPage';
import { SettingsPage } from '@/pages/SettingsPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, [pathname]);
  return null;
}

export function App() {
  return (
    <AppShell>
      <ScrollToTop />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/movies" element={<MoviesPage />} />
        <Route path="/series" element={<SeriesPage />} />
        <Route path="/genres" element={<GenresPage />} />
        <Route path="/genres/:genreId" element={<GenreDetailPage />} />
        <Route path="/my-list" element={<MyListPage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/title/:id" element={<DetailsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/settings/:section" element={<SettingsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AppShell>
  );
}
