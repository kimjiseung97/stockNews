import { Link, useNavigate } from 'react-router-dom'
import { useEffect, useState, type KeyboardEvent, type MouseEvent, type ReactNode } from 'react'
import { Search } from 'lucide-react'
import styles from '@/assets/styles/fixedContents/leftContents/leftContents.module.scss'
import { useAuth } from '@/contexts/AuthContext'
import { popularList, type PopularList } from '@/api/popular/popular'

interface LeftContentsProps {
  eyebrow: string
  headline: ReactNode
  description: ReactNode
}

export default function LeftContents({ eyebrow, headline, description }: LeftContentsProps) {
  const { email } = useAuth()
  const navigate = useNavigate()
  const [tiker, setTiker] = useState('')
  const [popularStock, setPopularStock] = useState<PopularList[]>([])
  function tikerOnKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') {
      tikerSearchBtn(e)
    }
  }

  function tikerSearchBtn(e: KeyboardEvent<HTMLInputElement> | MouseEvent<HTMLButtonElement>) {
    e.preventDefault()
    const koreaName = tiker.trim()

    navigate({
      pathname: email ? '/watchlist/register' : '/stock-search',
      search: koreaName ? `?${new URLSearchParams({ koreaName })}` : '',
    })
  }

  useEffect(() => {
    // 인기종목 목록 조회
    const getPopularStock = async () => {
      try {
        const response = await popularList(10)
        setPopularStock(response)
      } catch {
        setPopularStock([])
      }
    }

    void getPopularStock()
  }, [])
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
                id="sidebar-stock-search-query"
                type="text"
                placeholder={
                  email ? '관심종목을 검색해 주세요.' : '기업명 또는 티커를 입력해 주세요.'
                }
                value={tiker}
                onChange={(e) => setTiker(e.target.value)}
                onKeyDown={tikerOnKeyDown}
              />

              <button type="button" aria-label="종목 검색" onClick={tikerSearchBtn}>
                검색
              </button>
            </li>
            {/* <p>{tiker}</p> */}
            {popularStock.length > 0 && (
              <li className={styles['popular-stock-list']}>
                {popularStock.map((stock) => (
                  <button
                    type="button"
                    key={stock.id}
                    onClick={() => setTiker(stock.koreanName || stock.name)}
                  >
                    {stock.koreanName || stock.name}
                  </button>
                ))}
              </li>
            )}
          </ul>

          {!email && (
            <div className={styles['left-contents-container__account-actions']}>
              <Link to="/sign-up">시작하기</Link>
              <Link to="/login">로그인</Link>
            </div>
          )}
        </div>
      </section>
    </>
  )
}
