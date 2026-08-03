import { useEffect, useState } from 'react'
import { apiFetch, ApiError } from '@/lib/api'
import styles from '@/assets/styles/pages/watchlist/watchlist.module.scss'
import warningIcon from '@/assets/images/icons/x.png'

interface UserStockResponseData {
  id: number
  ticker: string
  name: string
  theme: string | null
  koreanName: string | null
}

interface PageResponseData<T> {
  content: T[]
  number: number
  totalPages: number
  totalElements: number
}

const PAGE_SIZE = 20

function WatchlistPage() {
  const [page, setPage] = useState(0)
  const [stocks, setStocks] = useState<UserStockResponseData[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    const fetchWatchlist = async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
        const data = await apiFetch<PageResponseData<UserStockResponseData>>(
          `/users/me/stocks?${params.toString()}`,
          { signal: controller.signal },
        )

        setStocks(data?.content ?? [])
        setTotalPages(data?.totalPages ?? 0)
        setTotalElements(data?.totalElements ?? 0)
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return
        }
        setErrorMessage(error instanceof ApiError ? error.message : '관심 종목 조회에 실패했습니다.')
      } finally {
        setIsLoading(false)
      }
    }

    void fetchWatchlist()

    return () => controller.abort()
  }, [page])

  return (
    <main id="watchlistPage" className={styles['watchlist-page']}>
      <section className={styles['watchlist-page__heading']}>
        <h1>관심 종목</h1>
        <p>등록한 관심 종목 목록입니다.</p>
      </section>

      {errorMessage && (
        <p className={styles['watchlist-page__notice']} role="alert">
          <img src={warningIcon} alt=""></img>
          {errorMessage}
        </p>
      )}

      {isLoading ? (
        <p className={styles['watchlist-page__status']}>불러오는 중입니다...</p>
      ) : stocks.length === 0 ? (
        <p className={styles['watchlist-page__status']}>등록된 관심 종목이 없습니다.</p>
      ) : (
        <>
          <ul className={styles['watchlist-page__list']}>
            {stocks.map((stock) => (
              <li key={stock.id} className={styles['watchlist-page__item']}>
                <span className={styles['watchlist-page__ticker']}>{stock.ticker}</span>
                <span className={styles['watchlist-page__names']}>
                  <strong>{stock.koreanName ?? stock.name}</strong>
                  {stock.koreanName && <small>{stock.name}</small>}
                </span>
                {stock.theme && (
                  <span className={styles['watchlist-page__theme']}>{stock.theme}</span>
                )}
              </li>
            ))}
          </ul>

          <nav className={styles['watchlist-page__pagination']} aria-label="관심 종목 페이지">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((current) => Math.max(current - 1, 0))}
            >
              이전
            </button>
            <span>
              {page + 1} / {Math.max(totalPages, 1)} 페이지 (총 {totalElements}건)
            </span>
            <button
              type="button"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((current) => current + 1)}
            >
              다음
            </button>
          </nav>
        </>
      )}
    </main>
  )
}

export default WatchlistPage
