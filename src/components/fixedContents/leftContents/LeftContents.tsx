import type { ReactNode } from 'react'
import { Search } from 'lucide-react'
import styles from '@/assets/styles/fixedContents/leftContents/leftContents.module.scss'

interface LeftContentsProps {
  eyebrow: string
  headline: ReactNode
  description: string
}

export default function LeftContents({ eyebrow, headline, description }: LeftContentsProps) {
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
              <input type="text" placeholder=" 티커 입력해 주세요." />
              <button type="button" aria-label="종목 검색">
                검색
              </button>
            </li>
            <li>태그</li>
          </ul>
        </div>
      </section>
    </>
  )
}
