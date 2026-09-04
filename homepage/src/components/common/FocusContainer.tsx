import { forwardRef, type ButtonHTMLAttributes, type ElementType, type ReactNode } from 'react';
import { useFocusable } from '@/navigation/useFocusable';

interface FocusContainerProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'id'> {
  id: string;
  onSelect?: () => void;
  disabled?: boolean;
  autoFocus?: boolean;
  as?: ElementType;
  children?: ReactNode;
  className?: string;
  focusedClassName?: string;
}

/**
 * The base focusable primitive every interactive control in the app is
 * built on: nav items, buttons, cards, list rows. Renders as a <button> by
 * default (correct semantics + native activation), exposes `data-focused`
 * so component CSS can style the D-pad-focused state distinctly from
 * mouse `:hover`.
 */
export const FocusContainer = forwardRef<HTMLButtonElement, FocusContainerProps>(function FocusContainer(
  { id, onSelect, disabled, autoFocus, as: As = 'button', children, className, focusedClassName, ...rest },
  forwardedRef,
) {
  const { ref, focused } = useFocusable(id, { onSelect, disabled, autoFocus });

  return (
    <As
      ref={(node: HTMLButtonElement | null) => {
        ref.current = node;
        if (typeof forwardedRef === 'function') forwardedRef(node);
        else if (forwardedRef) forwardedRef.current = node;
      }}
      id={id}
      type={As === 'button' ? 'button' : undefined}
      data-focused={focused || undefined}
      aria-disabled={disabled || undefined}
      className={`${className ?? ''} ${focused ? (focusedClassName ?? '') : ''}`.trim()}
      onClick={() => !disabled && onSelect?.()}
      {...rest}
    >
      {children}
    </As>
  );
});
