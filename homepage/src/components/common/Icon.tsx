import type { SVGProps } from 'react';

export type IconName =
  | 'home'
  | 'movies'
  | 'tv'
  | 'genres'
  | 'search'
  | 'my-list'
  | 'user'
  | 'settings'
  | 'play'
  | 'plus'
  | 'check'
  | 'info'
  | 'chevron-left'
  | 'chevron-right'
  | 'chevron-down'
  | 'close'
  | 'back'
  | 'star'
  | 'backspace'
  | 'volume'
  | 'subtitles'
  | 'language'
  | 'appearance'
  | 'playback'
  | 'account'
  | 'addon'
  | 'about'
  | 'error';

/**
 * A single, hand-built icon set (simple geometric strokes, 24x24) so the
 * app has no icon-font or third-party icon-library dependency to load or
 * ship.
 */
export function Icon({ name, ...rest }: { name: IconName } & SVGProps<SVGSVGElement>) {
  const common = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
    ...rest,
  };

  switch (name) {
    case 'home':
      return (
        <svg {...common}>
          <path d="M4 11.5 12 4l8 7.5" />
          <path d="M6 10v9a1 1 0 0 0 1 1h3v-6h4v6h3a1 1 0 0 0 1-1v-9" />
        </svg>
      );
    case 'movies':
      return (
        <svg {...common}>
          <rect x="3.5" y="4.5" width="17" height="15" rx="1.5" />
          <path d="M3.5 9h17M8 4.5 6 9M15 4.5 13 9" />
        </svg>
      );
    case 'tv':
      return (
        <svg {...common}>
          <rect x="3" y="5" width="18" height="13" rx="1.5" />
          <path d="M8 21h8M12 18v3" />
        </svg>
      );
    case 'genres':
      return (
        <svg {...common}>
          <rect x="3.5" y="3.5" width="7" height="7" rx="1" />
          <rect x="13.5" y="3.5" width="7" height="7" rx="1" />
          <rect x="3.5" y="13.5" width="7" height="7" rx="1" />
          <rect x="13.5" y="13.5" width="7" height="7" rx="1" />
        </svg>
      );
    case 'search':
      return (
        <svg {...common}>
          <circle cx="10.5" cy="10.5" r="6.5" />
          <path d="m20 20-4.6-4.6" />
        </svg>
      );
    case 'my-list':
      return (
        <svg {...common}>
          <path d="M6 3.5h12a.5.5 0 0 1 .5.5v16.5L12 17l-6.5 3.5V4a.5.5 0 0 1 .5-.5Z" />
        </svg>
      );
    case 'user':
      return (
        <svg {...common}>
          <circle cx="12" cy="8" r="3.5" />
          <path d="M4.5 20c1.4-3.6 4.4-5.5 7.5-5.5s6.1 1.9 7.5 5.5" />
        </svg>
      );
    case 'settings':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="3" />
          <path d="M12 3.5v2M12 18.5v2M4.9 6.4l1.4 1.4M17.7 16.2l1.4 1.4M3.5 12h2M18.5 12h2M4.9 17.6l1.4-1.4M17.7 7.8l1.4-1.4" />
        </svg>
      );
    case 'play':
      return (
        <svg {...common} fill="currentColor" stroke="none">
          <path d="M7 4.5v15l13-7.5Z" />
        </svg>
      );
    case 'plus':
      return (
        <svg {...common}>
          <path d="M12 5v14M5 12h14" />
        </svg>
      );
    case 'check':
      return (
        <svg {...common}>
          <path d="m4.5 12.5 5 5 10-11" />
        </svg>
      );
    case 'info':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M12 11v6M12 7.5v.01" />
        </svg>
      );
    case 'chevron-left':
      return (
        <svg {...common}>
          <path d="m15 5-7 7 7 7" />
        </svg>
      );
    case 'chevron-right':
      return (
        <svg {...common}>
          <path d="m9 5 7 7-7 7" />
        </svg>
      );
    case 'chevron-down':
      return (
        <svg {...common}>
          <path d="m5 9 7 7 7-7" />
        </svg>
      );
    case 'close':
      return (
        <svg {...common}>
          <path d="m5 5 14 14M19 5 5 19" />
        </svg>
      );
    case 'back':
      return (
        <svg {...common}>
          <path d="M19 12H6M11 6l-6 6 6 6" />
        </svg>
      );
    case 'star':
      return (
        <svg {...common} fill="currentColor" stroke="none">
          <path d="m12 3 2.7 5.9 6.3.7-4.7 4.4 1.3 6.3L12 17.2l-5.6 3.1 1.3-6.3-4.7-4.4 6.3-.7Z" />
        </svg>
      );
    case 'backspace':
      return (
        <svg {...common}>
          <path d="M9 5h11a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H9l-6-7Z" />
          <path d="m11 10 5 5m0-5-5 5" />
        </svg>
      );
    case 'volume':
      return (
        <svg {...common}>
          <path d="M4 9.5v5h4l5 4v-13l-5 4Z" />
          <path d="M16.5 9.2a4.5 4.5 0 0 1 0 5.6" />
        </svg>
      );
    case 'subtitles':
      return (
        <svg {...common}>
          <rect x="3" y="5.5" width="18" height="13" rx="1.5" />
          <path d="M6.5 14.5h4M13 14.5h4.5M6.5 10.5h11" />
        </svg>
      );
    case 'language':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M3 12h18M12 3c2.4 2.5 3.6 5.6 3.6 9s-1.2 6.5-3.6 9c-2.4-2.5-3.6-5.6-3.6-9S9.6 5.5 12 3Z" />
        </svg>
      );
    case 'appearance':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M12 3a9 9 0 0 0 0 18Z" fill="currentColor" stroke="none" />
        </svg>
      );
    case 'playback':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M10 8.5v7l6-3.5Z" fill="currentColor" stroke="none" />
        </svg>
      );
    case 'account':
      return (
        <svg {...common}>
          <circle cx="12" cy="8" r="3.5" />
          <path d="M4.5 20c1.4-3.6 4.4-5.5 7.5-5.5s6.1 1.9 7.5 5.5" />
        </svg>
      );
    case 'addon':
      return (
        <svg {...common}>
          <path d="M12 3.5 4 8v8l8 4.5 8-4.5V8Z" />
          <path d="M4 8l8 4.5L20 8M12 12.5V21" />
        </svg>
      );
    case 'about':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7.5v.01M11 11h1v6h1" />
        </svg>
      );
    case 'error':
      return (
        <svg {...common}>
          <path d="M12 3.5 21 19H3Z" />
          <path d="M12 9.5v4M12 16v.01" />
        </svg>
      );
    default:
      return null;
  }
}
