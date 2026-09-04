import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import { App } from './App';
import { SpatialNavProvider } from './navigation/SpatialNavContext';
import './styles/global.css';

// HashRouter, not BrowserRouter: this build is loaded from a
// file:///android_asset/ URL inside Mango-TV's WebView, with no server
// behind it to fall back arbitrary paths to index.html the way the History
// API needs. Hash-based routes never leave the one loaded document, so they
// work the same over file:// as they do served from a real origin.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <HashRouter>
      <SpatialNavProvider>
        <App />
      </SpatialNavProvider>
    </HashRouter>
  </StrictMode>,
);
