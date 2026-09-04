import { ErrorState } from '@/components/common/ErrorState';

export function NotFoundPage() {
  return (
    <div style={{ paddingTop: 'var(--mango-nav-height)' }}>
      <ErrorState title="Page not found" message="That page doesn't exist." />
    </div>
  );
}
