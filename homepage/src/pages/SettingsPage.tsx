import { useState, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FocusContainer } from '@/components/common/FocusContainer';
import { Icon, type IconName } from '@/components/common/Icon';
import { contentProvider } from '@/data/providerRegistry';
import styles from './SettingsPage.module.css';

type SectionId = 'playback' | 'appearance' | 'language' | 'subtitles' | 'audio' | 'account' | 'addons' | 'about';

const SECTIONS: { id: SectionId; label: string; icon: IconName }[] = [
  { id: 'playback', label: 'Playback', icon: 'playback' },
  { id: 'appearance', label: 'Appearance', icon: 'appearance' },
  { id: 'language', label: 'Language', icon: 'language' },
  { id: 'subtitles', label: 'Subtitles', icon: 'subtitles' },
  { id: 'audio', label: 'Audio', icon: 'volume' },
  { id: 'account', label: 'Account', icon: 'account' },
  { id: 'addons', label: 'Add-ons', icon: 'addon' },
  { id: 'about', label: 'About', icon: 'about' },
];

function Row({ label, description, control }: { label: string; description?: string; control: ReactNode }) {
  return (
    <div className={styles.row}>
      <div>
        <div className={styles.rowLabel}>{label}</div>
        {description && <div className={styles.rowDescription}>{description}</div>}
      </div>
      {control}
    </div>
  );
}

function OptionGroup({
  idPrefix,
  options,
  value,
  onChange,
}: {
  idPrefix: string;
  options: string[];
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className={styles.optionGroup}>
      {options.map((opt) => (
        <FocusContainer
          key={opt}
          id={`${idPrefix}-${opt}`}
          className={styles.option}
          data-active={value === opt}
          onSelect={() => onChange(opt)}
        >
          {opt}
        </FocusContainer>
      ))}
    </div>
  );
}

function Toggle({ id, active, onToggle }: { id: string; active: boolean; onToggle: () => void }) {
  return (
    <FocusContainer id={id} className={styles.toggle} data-active={active} onSelect={onToggle} aria-label="Toggle" />
  );
}

function PlaybackSection() {
  const [autoplay, setAutoplay] = useState(true);
  const [quality, setQuality] = useState('Auto');
  const [skipIntro, setSkipIntro] = useState(true);

  return (
    <div className={styles.group}>
      <Row
        label="Autoplay next episode"
        description="Automatically start the next episode when one finishes."
        control={<Toggle id="playback-autoplay" active={autoplay} onToggle={() => setAutoplay((v) => !v)} />}
      />
      <Row
        label="Streaming quality"
        description="Higher quality uses more bandwidth."
        control={
          <OptionGroup idPrefix="playback-quality" options={['Auto', '1080p', '720p', 'Data Saver']} value={quality} onChange={setQuality} />
        }
      />
      <Row
        label="Skip intros"
        description="Automatically skip opening titles when detected."
        control={<Toggle id="playback-skip-intro" active={skipIntro} onToggle={() => setSkipIntro((v) => !v)} />}
      />
    </div>
  );
}

function AppearanceSection() {
  const [theme, setTheme] = useState('Cinematic Dark');
  const [density, setDensity] = useState('Comfortable');

  return (
    <div className={styles.group}>
      <Row
        label="Theme"
        description="Mango is designed for a dark, cinematic theme."
        control={<OptionGroup idPrefix="appearance-theme" options={['Cinematic Dark', 'Pure Black']} value={theme} onChange={setTheme} />}
      />
      <Row
        label="Row density"
        description="How much artwork is visible per row."
        control={<OptionGroup idPrefix="appearance-density" options={['Comfortable', 'Compact']} value={density} onChange={setDensity} />}
      />
    </div>
  );
}

function LanguageSection() {
  const [language, setLanguage] = useState('English');
  return (
    <div className={styles.group}>
      <Row
        label="App language"
        control={<OptionGroup idPrefix="language" options={['English', 'Spanish', 'French', 'German']} value={language} onChange={setLanguage} />}
      />
    </div>
  );
}

function SubtitlesSection() {
  const [subtitles, setSubtitles] = useState(true);
  const [size, setSize] = useState('Medium');
  return (
    <div className={styles.group}>
      <Row
        label="Subtitles"
        description="Show subtitles when available."
        control={<Toggle id="subtitles-enabled" active={subtitles} onToggle={() => setSubtitles((v) => !v)} />}
      />
      <Row label="Subtitle size" control={<OptionGroup idPrefix="subtitles-size" options={['Small', 'Medium', 'Large']} value={size} onChange={setSize} />} />
    </div>
  );
}

function AudioSection() {
  const [audio, setAudio] = useState('Original');
  const [surround, setSurround] = useState(false);
  return (
    <div className={styles.group}>
      <Row label="Audio track" control={<OptionGroup idPrefix="audio-track" options={['Original', 'English', 'Spanish']} value={audio} onChange={setAudio} />} />
      <Row
        label="Surround sound"
        description="Use 5.1 surround when the source supports it."
        control={<Toggle id="audio-surround" active={surround} onToggle={() => setSurround((v) => !v)} />}
      />
    </div>
  );
}

function AccountSection() {
  return (
    <div className={styles.group}>
      <Row label="Signed in as" control={<span className={styles.rowDescription}>guest@mango.tv</span>} />
      <Row label="Manage profiles" control={<FocusContainer id="account-profiles" className={styles.option} onSelect={() => {}}>Manage</FocusContainer>} />
      <Row label="Sign out" control={<FocusContainer id="account-signout" className={styles.option} onSelect={() => {}}>Sign Out</FocusContainer>} />
    </div>
  );
}

function AddonsSection() {
  return (
    <div>
      <div className={styles.addonRow}>
        <div className={styles.addonMeta}>
          <div className={styles.rowLabel}>{contentProvider.displayName}</div>
          <div className={styles.rowDescription}>Bundled sample content — the default source until a real add-on is installed.</div>
        </div>
        <span className={styles.addonStatus}>Active</span>
      </div>
      <p className={styles.rowDescription}>
        Mango-Homepage's content layer is built around a provider abstraction (see ContentProvider), the same
        seam Mango-TV uses for its Stremio-compatible add-ons. Installing add-ons by manifest URL isn't wired up
        in this UI-only build yet.
      </p>
    </div>
  );
}

function AboutSection() {
  return (
    <div className={styles.about}>
      <p>Mango — a premium streaming interface, designed cinematic-first for Fire TV.</p>
      <p>Version 0.1.0 (frontend only, mock content)</p>
    </div>
  );
}

export function SettingsPage() {
  const navigate = useNavigate();
  const { section = 'playback' } = useParams<{ section?: SectionId }>();
  const active = (SECTIONS.find((s) => s.id === section)?.id ?? 'playback') as SectionId;

  return (
    <div className={styles.layout}>
      <nav className={styles.sidebar}>
        {SECTIONS.map((s) => (
          <FocusContainer
            key={s.id}
            id={`settings-nav-${s.id}`}
            className={styles.navItem}
            data-active={active === s.id}
            onSelect={() => navigate(`/settings/${s.id}`)}
          >
            <Icon name={s.icon} />
            {s.label}
          </FocusContainer>
        ))}
      </nav>
      <div className={styles.panel}>
        <h1 className={styles.panelTitle}>{SECTIONS.find((s) => s.id === active)?.label}</h1>
        {active === 'playback' && <PlaybackSection />}
        {active === 'appearance' && <AppearanceSection />}
        {active === 'language' && <LanguageSection />}
        {active === 'subtitles' && <SubtitlesSection />}
        {active === 'audio' && <AudioSection />}
        {active === 'account' && <AccountSection />}
        {active === 'addons' && <AddonsSection />}
        {active === 'about' && <AboutSection />}
      </div>
    </div>
  );
}
