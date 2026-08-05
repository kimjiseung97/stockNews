import { useEffect, useState } from 'react'
import { Search } from 'lucide-react'
import { apiFetch, ApiError } from '@/api/common/commonApi'
import styles from '@/assets/styles/pages/stock-search/stockSearch.module.scss'
import warningIcon from '@/assets/images/icons/x.png'

interface StockResponseData {
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
const SEARCH_DEBOUNCE_MS = 300

function StockSearchPage() {
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [stocks, setStocks] = useState<StockResponseData[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  // 검색어가 바뀌면 첫 페이지부터 다시 조회
  useEffect(() => {
    setPage(0)
  }, [keyword])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const controller = new AbortController()

      const search = async () => {
        setIsLoading(true)
        setErrorMessage('')

        try {
          const params = new URLSearchParams({
            page: String(page),
            size: String(PAGE_SIZE),
          })
          if (keyword.trim()) {
            params.set('keyword', keyword.trim())
          }

          const data = await apiFetch<PageResponseData<StockResponseData>>(
            `/stocks?${params.toString()}`,
            { signal: controller.signal },
          )

          setStocks(data?.content ?? [])
          setTotalPages(data?.totalPages ?? 0)
          setTotalElements(data?.totalElements ?? 0)
        } catch (error) {
          if (error instanceof DOMException && error.name === 'AbortError') {
            return
          }
          setErrorMessage(error instanceof ApiError ? error.message : '종목 검색에 실패했습니다.')
        } finally {
          setIsLoading(false)
        }
      }

      void search()

      return () => controller.abort()
    }, SEARCH_DEBOUNCE_MS)

    return () => window.clearTimeout(timer)
  }, [keyword, page])

  return (
    <main id="stockSearchPage" className={styles['stock-search-page']}>
      <section className={styles['stock-search-page__heading']}>
        <h1>관심 종목 검색</h1>
        <p>종목명(한글)으로 검색하세요.</p>
      </section>

      <label className={styles['stock-search-page__search-box']}>
        <input
          type="text"
          name="keyword"
          value={keyword}
          placeholder="예: 삼성전자, 애플"
          onChange={(event) => setKeyword(event.target.value)}
        />
        <Search aria-hidden="true"></Search>
      </label>

      {errorMessage && (
        <p className={styles['stock-search-page__notice']} role="alert">
          <img src={warningIcon} alt=""></img>
          {errorMessage}
        </p>
      )}

      {isLoading ? (
        <p className={styles['stock-search-page__status']}>검색 중입니다...</p>
      ) : stocks.length === 0 ? (
        <p className={styles['stock-search-page__status']}>검색 결과가 없습니다.</p>
      ) : (
        <>
          <ul className={styles['stock-search-page__list']}>
            {stocks.map((stock) => (
              <li key={stock.id} className={styles['stock-search-page__item']}>
                <span className={styles['stock-search-page__ticker']}>{stock.ticker}</span>
                <span className={styles['stock-search-page__names']}>
                  <strong>{stock.koreanName ?? stock.name}</strong>
                  {stock.koreanName && <small>{stock.name}</small>}
                </span>
                {stock.theme && (
                  <span className={styles['stock-search-page__theme']}>{stock.theme}</span>
                )}
              </li>
            ))}
          </ul>

          <nav className={styles['stock-search-page__pagination']} aria-label="검색 결과 페이지">
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

export default StockSearchPage
