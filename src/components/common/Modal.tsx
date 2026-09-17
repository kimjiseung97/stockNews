import { useEffect, useRef, useState, type MouseEvent, type ReactNode, type RefObject } from 'react'
import { X } from 'lucide-react'
import styles from '@/assets/styles/common/modal.module.scss'

interface ModalProps {
  titleId: string
  title: string
  onClose: () => void
  restoreFocusRef: RefObject<HTMLElement | null>
  children: ReactNode
}

export default function Modal({ titleId, title, onClose, restoreFocusRef, children }: ModalProps) {
  const titleRef = useRef<HTMLHeadingElement>(null)
  const [isClosing, setIsClosing] = useState(false)

  // 닫힘 애니메이션 시작
  const handleClose = () => {
    setIsClosing(true)
  }

  // 모달을 열고 닫을 때 키보드 초점 이동
  useEffect(() => {
    titleRef.current?.focus()

    return () => {
      requestAnimationFrame(() => restoreFocusRef.current?.focus())
    }
  }, [restoreFocusRef])

  const handleOverlayClick = (event: MouseEvent<HTMLDivElement>) => {
    if (event.target === event.currentTarget) handleClose()
  }

  return (
    <div
      className={`${styles['modal-overlay']} ${isClosing ? styles['modal-overlay--closing'] : ''}`}
      onClick={handleOverlayClick}
    >
      <dialog
        className={`${styles['modal']} ${isClosing ? styles['modal--closing'] : ''}`}
        open
        aria-labelledby={titleId}
        onAnimationEnd={(event) => {
          if (isClosing && event.target === event.currentTarget) onClose()
        }}
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            event.stopPropagation()
            handleClose()
          }
        }}
      >
        <div className={styles['modal__header']}>
          <h2 id={titleId} className={styles['modal__title']} ref={titleRef} tabIndex={-1}>
            {title}
          </h2>
          <button
            className={styles['modal__close']}
            type="button"
            aria-label="닫기"
            onClick={handleClose}
          >
            <X size={20} aria-hidden="true"></X>
          </button>
        </div>

        <div className={styles['modal__body']}>{children}</div>
      </dialog>
    </div>
  )
}
