import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
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
  const location = useLocation()
  const [searchParams] = useSearchParams()
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

  // 인기종목 바로 검색
  function popularStockSearchBtn(stockName: string) {
    setTiker(stockName)
    navigate({
      pathname: '/stock-news',
      search: `?${new URLSearchParams({ koreaName: stockName })}`,
    })
  }

  // 종목별 뉴스 화면에서 조회 중인 종목과 검색창 동기화
  useEffect(() => {
    if (location.pathname === '/stock-news') {
      setTiker(searchParams.get('koreaName') ?? '')
    }
  }, [location.pathname, searchParams])

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
    <aside
      id="leftContentsContainer"
      className={styles['left-contents-container']}
      aria-labelledby="leftContentsTitle"
    >
      <h2 id="leftContentsTitle" className={styles['left-contents-container__title']}>
        {headline}
      </h2>
      <p className={styles['left-contents-container__eyebrow']}>{eyebrow}</p>
      <p className={styles['left-contents-container__description']}>{description}</p>

      <search className={styles['input-box']}>
        <label className={styles['left-contents-container__search-label']} htmlFor="sidebar-stock-search-query">
          어떤 종목의 뉴스를 찾고 있나요?
        </label>
        <p className={styles['left-contents-container__search-controls']}>
          <Search aria-hidden="true" color="#fff"></Search>
          <input
            id="sidebar-stock-search-query"
            type="search"
            placeholder={email ? '관심종목을 검색해 주세요.' : '기업명 또는 티커를 입력해 주세요.'}
            value={tiker}
            onChange={(e) => setTiker(e.target.value)}
            onKeyDown={tikerOnKeyDown}
          ></input>
          <button type="button" aria-label="종목 검색" onClick={tikerSearchBtn}>
            검색
          </button>
        </p>
        {popularStock.length > 0 && (
          <ul className={styles['popular-stock-list']} aria-label="인기종목">
            {popularStock.map((stock) => (
              <li key={stock.id}>
                <button
                  type="button"
                  onClick={() => popularStockSearchBtn(stock.koreanName || stock.name)}
                >
                  {stock.koreanName || stock.name}
                </button>
              </li>
            ))}
          </ul>
        )}
      </search>

      {!email && (
        <p className={styles['left-contents-container__account-actions']}>
          <Link to="/sign-up">시작하기</Link>
          <Link to="/login">로그인</Link>
        </p>
      )}
    </aside>
  )
}
