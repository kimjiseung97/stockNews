import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ArrowUp, X } from 'lucide-react'
import { askStockChat } from '@/api/chat/chat'
import { useAuth } from '@/contexts/AuthContext'
import chatbotImage from '@/assets/images/icons/chatbot1.webp'
import chatbotAnswerImage from '@/assets/images/icons/chatbot2.webp'
import chatbotLoadingImage from '@/assets/images/icons/chatbot3.webp'
import styles from '@/assets/styles/fixedContents/stockChat/stockChat.module.scss'
import mediaStyles from '@/assets/styles/fixedContents/stockChat/stockChatMedia.module.scss'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
}

export default function StockChat() {
  const { email } = useAuth()
  const [isOpen, setIsOpen] = useState(false)
  const [question, setQuestion] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [inputError, setInputError] = useState(false)
  const titleRef = useRef<HTMLHeadingElement>(null)
  const toggleButtonRef = useRef<HTMLButtonElement>(null)
  const stockChatRef = useRef<HTMLElement>(null)

  // 푸터가 보이면 챗봇을 푸터 위로 이동
  useEffect(() => {
    const stockChat = stockChatRef.current
    const footer = document.querySelector<HTMLElement>('#mainFooter')
    const scrollContainer = footer?.parentElement
    const mobileMediaQuery = window.matchMedia('(max-width: 1024px)')

    if (!stockChat || !footer || !scrollContainer) return

    let animationFrameId = 0

    const updateFooterOffset = () => {
      cancelAnimationFrame(animationFrameId)

      animationFrameId = requestAnimationFrame(() => {
        if (!mobileMediaQuery.matches) {
          stockChat.style.setProperty('--stock-chat-footer-offset', '0px')
          return
        }

        const footerRect = footer.getBoundingClientRect()
        const scrollContainerRect = scrollContainer.getBoundingClientRect()
        const footerOverlap = Math.max(0, scrollContainerRect.bottom - footerRect.top)

        stockChat.style.setProperty('--stock-chat-footer-offset', `${footerOverlap}px`)
      })
    }

    updateFooterOffset()
    scrollContainer.addEventListener('scroll', updateFooterOffset, { passive: true })
    window.addEventListener('resize', updateFooterOffset)
    mobileMediaQuery.addEventListener('change', updateFooterOffset)

    return () => {
      cancelAnimationFrame(animationFrameId)
      scrollContainer.removeEventListener('scroll', updateFooterOffset)
      window.removeEventListener('resize', updateFooterOffset)
      mobileMediaQuery.removeEventListener('change', updateFooterOffset)
    }
  }, [])

  // 채팅창을 열고 닫을 때 키보드 초점 이동
  useEffect(() => {
    if (!isOpen) return

    titleRef.current?.focus()

    return () => {
      requestAnimationFrame(() => toggleButtonRef.current?.focus())
    }
  }, [isOpen])

  // 질문 전송
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const trimmedQuestion = question.trim()

    if (!email || isLoading) return

    if (!trimmedQuestion) {
      setInputError(true)
      event.currentTarget.querySelector('textarea')?.focus()
      return
    }

    const userMessage: ChatMessage = {
      id: Date.now(),
      role: 'user',
      content: trimmedQuestion,
    }

    setMessages((currentMessages) => [...currentMessages, userMessage])
    setInputError(false)
    setQuestion('')
    setIsLoading(true)

    try {
      const answer = await askStockChat(trimmedQuestion)

      setMessages((currentMessages) => [
        ...currentMessages,
        {
          id: Date.now() + 1,
          role: 'assistant',
          content: answer,
        },
      ])
    } catch {
      setMessages((currentMessages) => [
        ...currentMessages,
        {
          id: Date.now() + 1,
          role: 'assistant',
          content: '답변을 불러오지 못했습니다. 잠시 후 다시 질문해 주세요.',
        },
      ])
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <aside
      ref={stockChatRef}
      id="stockChat"
      className={`${styles['stock-chat']} ${mediaStyles['stock-chat']}`}
      aria-label="주식 챗봇"
    >
      {isOpen && (
        <dialog
          id="stockChatPanel"
          className={styles['stock-chat__panel']}
          open
          aria-labelledby="stockChatTitle"
          onKeyDown={(event) => {
            if (event.key === 'Escape') {
              event.stopPropagation()
              setIsOpen(false)
            }
          }}
        >
          <div className={styles['stock-chat__header']}>
            <hgroup>
              <h2
                id="stockChatTitle"
                className={styles['stock-chat__title']}
                ref={titleRef}
                tabIndex={-1}
              >
                주식 챗봇 모아
              </h2>
              <p className={styles['stock-chat__status']}>
                <span aria-hidden="true"></span>
                질문할 수 있어요
              </p>
            </hgroup>

            <button
              className={styles['stock-chat__close']}
              type="button"
              aria-label="채팅창 닫기"
              onClick={() => setIsOpen(false)}
            >
              <X size={20} aria-hidden="true"></X>
            </button>
          </div>

          <ul
            className={styles['stock-chat__messages']}
            aria-label="주식 챗봇 대화"
            aria-live="polite"
            aria-relevant="additions text"
            tabIndex={0}
          >
            <li className={styles['stock-chat__welcome']}>
              <p className={styles['stock-chat__message-author']}>
                <img
                  className={styles['stock-chat__message-profile']}
                  src={chatbotAnswerImage}
                  alt=""
                ></img>
                <strong>모아</strong>
              </p>
              <span className={styles['stock-chat__answer-content']}>
                아래 예시처럼 구체적으로 질문해 보세요!
                <br></br>“엔비디아의 최신 소식, 실적이나 공시,
                <br></br> 최근 발표된 뉴스를 알려줄래?”
              </span>
            </li>

            {messages.map((message) => (
              <li
                key={message.id}
                className={`${styles['stock-chat__message']} ${
                  message.role === 'user'
                    ? styles['stock-chat__message--user']
                    : styles['stock-chat__message--assistant']
                }`}
              >
                {message.role === 'assistant' ? (
                  <>
                    <p className={styles['stock-chat__message-author']}>
                      <img
                        className={styles['stock-chat__message-profile']}
                        src={chatbotAnswerImage}
                        alt=""
                      ></img>
                      <strong>모아</strong>
                    </p>
                    <span className={styles['stock-chat__answer-content']}>{message.content}</span>
                  </>
                ) : (
                  <p>{message.content}</p>
                )}
              </li>
            ))}

            {isLoading && (
              <li
                className={`${styles['stock-chat__message']} ${styles['stock-chat__message--assistant']} ${styles['stock-chat__loading']}`}
              >
                <img
                  className={styles['stock-chat__message-profile']}
                  src={chatbotLoadingImage}
                  alt=""
                ></img>
                <p className={styles['stock-chat__loading-answer']} role="status">
                  <span className={styles['stock-chat__loading-dot']} aria-hidden="true"></span>
                  <span className={styles['stock-chat__loading-dot']} aria-hidden="true"></span>
                  <span className={styles['stock-chat__loading-dot']} aria-hidden="true"></span>
                  <span className={styles['stock-chat__label']}>
                    모아가 답변을 작성하고 있습니다
                  </span>
                </p>
              </li>
            )}
          </ul>

          <form className={styles['stock-chat__form']} onSubmit={handleSubmit}>
            <label className={styles['stock-chat__input-wrap']}>
              <span className={styles['stock-chat__label']}>질문 입력</span>
              <textarea
                id="stockChatQuestion"
                className={styles['stock-chat__input']}
                value={question}
                rows={1}
                maxLength={1000}
                placeholder={email ? '모아에게 질문해 주세요.' : '로그인 후 이용 가능합니다.'}
                disabled={!email || isLoading}
                aria-invalid={inputError}
                aria-describedby={inputError ? 'stockChatInputError' : undefined}
                onChange={(event) => {
                  setQuestion(event.target.value)

                  if (inputError) setInputError(false)
                }}
              ></textarea>
            </label>
            {inputError && (
              <p
                id="stockChatInputError"
                className={styles['stock-chat__input-error']}
                role="alert"
              >
                질문을 입력해 주세요.
              </p>
            )}
            <button
              className={styles['stock-chat__send']}
              type="submit"
              aria-label="질문 보내기"
              disabled={!email || isLoading}
            >
              <ArrowUp size={21} aria-hidden="true"></ArrowUp>
            </button>
          </form>
        </dialog>
      )}

      {!isOpen && (
        <button
          ref={toggleButtonRef}
          className={`${styles['stock-chat__floating-button']} ${mediaStyles['stock-chat__floating-button']}`}
          type="button"
          aria-label="무엇이든 물어보세요. 주식 AI 채팅 열기"
          aria-haspopup="dialog"
          onClick={() => setIsOpen(true)}
        >
          <span>무엇이든 물어보세요</span>
          <img src={chatbotImage} alt=""></img>
        </button>
      )}
    </aside>
  )
}
