import { useState, type FormEvent } from 'react'
import { ArrowUp, X } from 'lucide-react'
import { askStockChat } from '@/api/chat/chat'
import { useAuth } from '@/contexts/AuthContext'
import chatbotImage from '@/assets/images/icons/chatbot1.png'
import chatbotAnswerImage from '@/assets/images/icons/chatbot2.png'
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
    <section id="stockChat" className={`${styles['stock-chat']} ${mediaStyles['stock-chat']}`}>
      {isOpen && (
        <article
          id="stockChatPanel"
          className={styles['stock-chat__panel']}
          role="dialog"
          aria-modal="false"
          aria-labelledby="stockChatTitle"
        >
          <section className={styles['stock-chat__header']}>
            <section className={styles['stock-chat__title-wrap']}>
              <section>
                <h2 id="stockChatTitle" className={styles['stock-chat__title']}>
                  주식 챗봇 모아
                </h2>
                <p className={styles['stock-chat__status']}>
                  <span aria-hidden="true"></span>
                  질문할 수 있어요
                </p>
              </section>
            </section>

            <button
              className={styles['stock-chat__close']}
              type="button"
              aria-label="채팅창 닫기"
              onClick={() => setIsOpen(false)}
            >
              <X size={20}></X>
            </button>
          </section>

          <ul className={styles['stock-chat__messages']} aria-live="polite">
            <li className={styles['stock-chat__welcome']}>
              <img
                className={styles['stock-chat__message-profile']}
                src={chatbotAnswerImage}
                alt=""
              ></img>
              <section className={styles['stock-chat__answer-content']}>
                <strong>모아</strong>
                <p>
                  아래 예시처럼 구체적으로 질문해 보세요!
                  <br></br>“엔비디아의 최신 소식, 실적이나 공시,
                  <br></br> 최근 발표된 뉴스를 알려줄래?”
                </p>
              </section>
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
                    <img
                      className={styles['stock-chat__message-profile']}
                      src={chatbotAnswerImage}
                      alt=""
                    ></img>
                    <section className={styles['stock-chat__answer-content']}>
                      <strong>모아</strong>
                      <p>{message.content}</p>
                    </section>
                  </>
                ) : (
                  <p>{message.content}</p>
                )}
              </li>
            ))}

            {isLoading && (
              <li
                className={`${styles['stock-chat__message']} ${styles['stock-chat__message--assistant']} ${styles['stock-chat__loading']}`}
                aria-label="답변을 작성하고 있습니다"
              >
                <img
                  className={styles['stock-chat__message-profile']}
                  src={chatbotAnswerImage}
                  alt=""
                ></img>
                <section
                  className={styles['stock-chat__loading-answer']}
                  aria-label="모아가 답변을 작성하고 있습니다"
                >
                  <span className={styles['stock-chat__loading-dot']} aria-hidden="true"></span>
                  <span className={styles['stock-chat__loading-dot']} aria-hidden="true"></span>
                  <span className={styles['stock-chat__loading-dot']} aria-hidden="true"></span>
                </section>
              </li>
            )}
          </ul>

          <form className={styles['stock-chat__form']} onSubmit={handleSubmit}>
            <label className={styles['stock-chat__input-wrap']}>
              <span className={styles['stock-chat__label']}>질문 입력</span>
              <textarea
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
              <ArrowUp size={21}></ArrowUp>
            </button>
          </form>
        </article>
      )}

      {!isOpen && (
        <button
          className={styles['stock-chat__floating-button']}
          type="button"
          aria-label="주식 AI 채팅 열기"
          aria-expanded={isOpen}
          aria-controls="stockChatPanel"
          onClick={() => setIsOpen(true)}
        >
          <span>무엇이든 물어보세요</span>
          <img src={chatbotImage} alt=""></img>
        </button>
      )}
    </section>
  )
}
