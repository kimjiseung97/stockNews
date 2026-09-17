import { useRef, useState } from 'react'
import Modal from '@/components/common/Modal'
import { ContactContent, PrivacyContent, TermsContent } from './PolicyContent'
import styles from '@/assets/styles/fixedContents/footer/footer.module.scss'
import mediaStyles from '@/assets/styles/fixedContents/footer/footerMedia.module.scss'

type FooterModalType = 'terms' | 'privacy' | 'contact'

const currentYear = new Date().getFullYear()

export default function Footer() {
  const [openModal, setOpenModal] = useState<FooterModalType | null>(null)
  const termsButtonRef = useRef<HTMLButtonElement>(null)
  const privacyButtonRef = useRef<HTMLButtonElement>(null)
  const contactButtonRef = useRef<HTMLButtonElement>(null)

  const modalConfig = {
    terms: {
      title: '이용약관',
      buttonRef: termsButtonRef,
      content: <TermsContent></TermsContent>,
    },
    privacy: {
      title: '개인정보처리방침',
      buttonRef: privacyButtonRef,
      content: <PrivacyContent></PrivacyContent>,
    },
    contact: {
      title: '협업 및 광고 제안',
      buttonRef: contactButtonRef,
      content: <ContactContent></ContactContent>,
    },
  } as const

  return (
    <footer id="mainFooter" className={`${styles['main-footer']} ${mediaStyles['main-footer']}`}>
      <nav className={styles['main-footer__links']} aria-label="정책 및 문의">
        <ul
          className={`${styles['main-footer__link-list']} ${mediaStyles['main-footer__link-list']}`}
        >
          <li className={styles['main-footer__link-item']}>
            <button
              className={styles['main-footer__link-button']}
              ref={termsButtonRef}
              type="button"
              onClick={() => setOpenModal('terms')}
            >
              이용약관
            </button>
          </li>
          <li className={styles['main-footer__link-item']}>
            <button
              className={styles['main-footer__link-button']}
              ref={privacyButtonRef}
              type="button"
              onClick={() => setOpenModal('privacy')}
            >
              개인정보처리방침
            </button>
          </li>
          <li className={styles['main-footer__link-item']}>
            <button
              className={styles['main-footer__link-button']}
              ref={contactButtonRef}
              type="button"
              onClick={() => setOpenModal('contact')}
            >
              협업 및 광고 제안
            </button>
          </li>
        </ul>
      </nav>

      <p className={styles['main-footer__copyright']}>
        &copy; {currentYear} StockNews. All rights reserved.
      </p>

      {openModal && (
        <Modal
          titleId="footerModalTitle"
          title={modalConfig[openModal].title}
          onClose={() => setOpenModal(null)}
          restoreFocusRef={modalConfig[openModal].buttonRef}
        >
          {modalConfig[openModal].content}
        </Modal>
      )}
    </footer>
  )
}
