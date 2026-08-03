import { Link } from 'react-router-dom'
import { useState, type KeyboardEvent, type MouseEvent, type ReactNode } from 'react'
import { Search } from 'lucide-react'
import styles from '@/assets/styles/fixedContents/leftContents/leftContents.module.scss'

interface LeftContentsProps {
  eyebrow: string
  headline: ReactNode
  description: ReactNode
}

export default function LeftContents({ eyebrow, headline, description }: LeftContentsProps) {
  const [tiker, setTiker] = useState('')
  function tikerOnKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') {
      tikerSearchBtn(e)
    }
  }

  function tikerSearchBtn(e: KeyboardEvent<HTMLInputElement> | MouseEvent<HTMLButtonElement>) {
    void e
  }
  return (
    <>
      <section id="leftContentsContainer" className={styles['left-contents-container']}>
        <div className={styles['left-contents-container__inner']}>
          <h1>{eyebrow}</h1>
          <article>
            <h2>{headline}</h2>

            <p>{description}</p>
          </article>

          <ul className={styles['input-box']}>
            <li>어떤 종목의 뉴스를 찾고 있나요?</li>
            <li>
              <Search aria-hidden="true" color="#fff"></Search>
              <input
                type="text"
                placeholder=" 티커 입력해 주세요."
                value={tiker}
                onChange={(e) => setTiker(e.target.value)}
                onKeyDown={tikerOnKeyDown}
              />

              <button type="button" aria-label="종목 검색" onClick={tikerSearchBtn}>
                검색
              </button>
            </li>
            {/* <p>{tiker}</p> */}
            <li>태그</li>
          </ul>

          <div className={styles['left-contents-container__account-actions']}>
            <Link to="/sign-up">시작하기</Link>
            <Link to="/login">로그인</Link>
          </div>
        </div>
      </section>
    </>
  )
}
