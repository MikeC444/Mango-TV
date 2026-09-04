import { useEffect, type ReactNode } from 'react';
import { useSpatialNav } from '@/navigation/SpatialNavContext';
import { FocusContainer } from './FocusContainer';
import { Icon } from './Icon';
import styles from './Modal.module.css';

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

/** A focus-trapping modal: Back closes it (see the pushBackHandler below) instead of leaving the page. */
export function Modal({ title, onClose, children }: ModalProps) {
  const { pushBackHandler } = useSpatialNav();

  useEffect(() => pushBackHandler(() => {
    onClose();
    return true;
  }), [pushBackHandler, onClose]);

  return (
    <div className={styles.overlay} onClick={onClose} role="presentation">
      <div className={styles.panel} onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label={title}>
        <FocusContainer id="modal-close" className={styles.closeButton} onSelect={onClose} autoFocus aria-label="Close">
          <Icon name="close" />
        </FocusContainer>
        <div className={styles.title}>{title}</div>
        {children}
      </div>
    </div>
  );
}
