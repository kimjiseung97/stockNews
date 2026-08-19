import { useEffect, useState, type FormEvent } from 'react'
import { Search } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { ApiError } from '@/api/common/commonApi'
import { stockSearch, type StockSearchResponse } from '@/api/stockSearch/stockSearch'
import styles from '@/assets/styles/pages/stock-search/stockSearch.module.scss'
import mediaStyles from '@/assets/styles/pages/stock-search/stockSearchMedia.module.scss'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import ListSkeleton from '@/components/common/ListSkeleton'
import warningIcon from '@/assets/images/icons/x.png'

const PAGE_SIZE = 10

function StockSearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const koreaName = searchParams.get('koreaName')?.trim() ?? ''
  const [keyword, setKeyword] = useState(koreaName)
  const [searchedKeyword, setSearchedKeyword] = useState(koreaName)
  const [result, setResult] = useState<StockSearchResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSearching, setIsSearching] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const fetchStocks = async (searchKeyword: string, page: number) => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      setResult(await stockSearch({ keyword: searchKeyword, page, size: PAGE_SIZE }))
    } catch (error) {
      setResult(null)
      setErrorMessage(
        error instanceof ApiError ? error.message : '종목 목록을 불러오지 못했습니다.',
      )
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    setKeyword(koreaName)
    setSearchedKeyword(koreaName)
    void fetchStocks(koreaName, 0).finally(() => setIsSearching(false))
  }, [koreaName])

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmedKeyword = keyword.trim()
    setKeyword(trimmedKeyword)
    setSearchedKeyword(trimmedKeyword)
    setIsSearching(true)

    if (trimmedKeyword === koreaName) {
      void fetchStocks(trimmedKeyword, 0).finally(() => setIsSearching(false))
      return
    }

    setSearchParams(trimmedKeyword ? { koreaName: trimmedKeyword } : {})
  }

  return (
    <main
      id="stockSearchPage"
      className={`${styles['stock-search-page']} ${mediaStyles['stock-search-page']}`}
    >
      <section className={styles['stock-search-page__heading']}>
        <p className={styles['stock-search-page__eyebrow']}>STOCK SEARCH</p>
        <h1>종목 검색</h1>
        <p>미국 주식 티커 또는 기업명을 입력해 원하는 종목을 찾아보세요.</p>
      </section>

      <form className={styles['stock-search-page__search-form']} onSubmit={handleSubmit}>
        <label className={styles['stock-search-page__search-box']}>
          <Search aria-hidden="true"></Search>
          <span className={styles['stock-search-page__sr-only']}>티커 또는 기업명</span>
          <input
            id="stock-search-query"
            type="search"
            value={keyword}
            placeholder="예: 엔비디아, 애플"
            maxLength={100}
            autoComplete="off"
            onChange={(event) => setKeyword(event.target.value)}
          ></input>
        </label>
        <button type="submit" disabled={isLoading}>
          {isSearching ? <LoadingSpinner label="조회 중"></LoadingSpinner> : '조회'}
        </button>
      </form>

      {errorMessage && (
        <p className={styles['stock-search-page__notice']} role="alert">
          <img src={warningIcon} alt=""></img>
          {errorMessage}
        </p>
      )}

      {isLoading ? (
        <ListSkeleton count={6} label="종목 목록을 불러오는 중입니다."></ListSkeleton>
      ) : result && result.content.length > 0 ? (
        <>
          <p className={styles['stock-search-page__summary']}>
            총&nbsp;<strong>{result.totalElements.toLocaleString()}</strong>&nbsp;개의 종목
          </p>
          <ul className={styles['stock-search-page__list']}>
            {result.content.map((stock) => (
              <li className={styles['stock-search-page__item']} key={stock.id}>
                <span className={styles['stock-search-page__ticker']}>{stock.ticker}</span>
                <span className={styles['stock-search-page__names']}>
                  <strong>{stock.koreanName || stock.name}</strong>
                  {stock.koreanName && <small>{stock.name}</small>}
                </span>
                {stock.theme && (
                  <span className={styles['stock-search-page__theme']}>{stock.theme}</span>
                )}
              </li>
            ))}
          </ul>

          {result.totalPages > 1 && (
            <nav className={styles['stock-search-page__pagination']} aria-label="종목 목록 페이지">
              <button
                type="button"
                disabled={result.first || isLoading}
                onClick={() => void fetchStocks(searchedKeyword, result.number - 1)}
              >
                이전
              </button>
              <span>
                {result.number + 1} / {result.totalPages}
              </span>
              <button
                type="button"
                disabled={result.last || isLoading}
                onClick={() => void fetchStocks(searchedKeyword, result.number + 1)}
              >
                다음
              </button>
            </nav>
          )}
        </>
      ) : (
        !errorMessage && (
          <p className={styles['stock-search-page__status']}>
            {searchedKeyword
              ? `“${searchedKeyword}”에 해당하는 종목이 없습니다.`
              : '등록된 종목이 없습니다.'}
          </p>
        )
      )}
    </main>
  )
}

export default StockSearchPage
