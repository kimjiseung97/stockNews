import { useEffect, useState, type FormEvent } from 'react'
import { ArrowRight, ExternalLink, Search } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { ApiError } from '@/api/common/commonApi'
import { stockNews, type StockNewsResponse } from '@/api/stockNews/stockNews'
import { stockSearch, type Stock } from '@/api/stockSearch/stockSearch'
import ListSkeleton from '@/components/common/ListSkeleton'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import styles from '@/assets/styles/pages/stock-news/stockNews.module.scss'
import mediaStyles from '@/assets/styles/pages/stock-news/stockNewsMedia.module.scss'

const PAGE_SIZE = 10

function StockNewsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedStockName = searchParams.get('stockName') ?? ''
  const [stockId, setStockId] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [selectedTicker, setSelectedTicker] = useState('')
  const [keyword, setKeyword] = useState('')
  const [searchedStocks, setSearchedStocks] = useState<Stock[]>([])
  const [newsPage, setNewsPage] = useState<StockNewsResponse | null>(null)
  const [isStockSearching, setIsStockSearching] = useState(false)
  const [hasStockSearched, setHasStockSearched] = useState(false)
  const [isNewsLoading, setIsNewsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (!selectedStockName) {
      setStockId(0)
      setSelectedTicker('')
      setCurrentPage(0)
      return
    }

    const loadSelectedStock = async () => {
      try {
        const response = await stockSearch({ keyword: selectedStockName, page: 0, size: 10 })
        const selectedStock = response.content.find(
          (stock) => stock.koreanName === selectedStockName || stock.name === selectedStockName,
        )

        if (!selectedStock) {
          setStockId(0)
          setSelectedTicker('')
          setErrorMessage('선택한 종목을 찾지 못했습니다.')
          return
        }

        setStockId(selectedStock.stockId)
        setSelectedTicker(selectedStock.ticker)
      } catch (error) {
        setStockId(0)
        setSelectedTicker('')
        setErrorMessage(
          error instanceof ApiError ? error.message : '선택한 종목을 찾지 못했습니다.',
        )
      }
    }

    void loadSelectedStock()
  }, [selectedStockName])

  useEffect(() => {
    if (!Number.isInteger(stockId) || stockId <= 0) {
      setNewsPage(null)
      return
    }

    const loadNews = async () => {
      setIsNewsLoading(true)
      setErrorMessage('')

      try {
        setNewsPage(
          await stockNews({ stockId, page: currentPage, size: PAGE_SIZE }),
        )
      } catch (error) {
        setNewsPage(null)
        setErrorMessage(
          error instanceof ApiError ? error.message : '종목 뉴스를 불러오지 못했습니다.',
        )
      } finally {
        setIsNewsLoading(false)
      }
    }

    void loadNews()
  }, [currentPage, stockId])

  // 뉴스 조회 종목 검색
  const handleStockSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmedKeyword = keyword.trim()

    if (!trimmedKeyword) {
      setSearchedStocks([])
      setHasStockSearched(false)
      return
    }

    setKeyword(trimmedKeyword)
    setIsStockSearching(true)
    setHasStockSearched(true)
    setErrorMessage('')

    try {
      const response = await stockSearch({ keyword: trimmedKeyword, page: 0, size: 5 })
      setSearchedStocks(response.content)
    } catch (error) {
      setSearchedStocks([])
      setErrorMessage(
        error instanceof ApiError ? error.message : '종목을 검색하지 못했습니다.',
      )
    } finally {
      setIsStockSearching(false)
    }
  }

  // 선택한 종목의 첫 뉴스 페이지 조회
  const handleStockSelect = (stock: Stock) => {
    setStockId(stock.stockId)
    setSelectedTicker(stock.ticker)
    setCurrentPage(0)
    setSearchParams({
      stockName: stock.koreanName || stock.name,
    })
    setKeyword('')
    setSearchedStocks([])
    setHasStockSearched(false)
  }

  const handlePageChange = (page: number) => {
    setCurrentPage(page)
  }

  const getCollectedDate = (collectedAt: string) =>
    new Date(collectedAt).toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })

  return (
    <main
      id="stockNewsPage"
      className={`${styles['stock-news-page']} ${mediaStyles['stock-news-page']}`}
    >
      <section className={styles['stock-news-page__heading']}>
        <p className={styles['stock-news-page__eyebrow']}>STOCK NEWS</p>
        <h1>종목별 뉴스</h1>
        <p>궁금한 종목을 검색하고 최근 수집된 뉴스를 확인하세요.</p>
      </section>

      <form className={styles['stock-news-page__search-form']} onSubmit={handleStockSearch}>
        <label className={styles['stock-news-page__search-box']}>
          <Search aria-hidden="true"></Search>
          <span className={styles['stock-news-page__sr-only']}>종목 검색</span>
          <input
            type="search"
            value={keyword}
            placeholder="종목을 검색해 주세요"
            maxLength={100}
            autoComplete="off"
            onChange={(event) => setKeyword(event.target.value)}
          ></input>
        </label>
        <button type="submit" disabled={isStockSearching}>
          {isStockSearching ? <LoadingSpinner label="검색 중"></LoadingSpinner> : '검색'}
        </button>
      </form>

      {isStockSearching ? (
        <ListSkeleton count={5} label="종목을 검색하는 중입니다."></ListSkeleton>
      ) : hasStockSearched ? (
        <section className={styles['stock-news-page__stock-result']}>
          <h2>
            검색 결과 <strong>{searchedStocks.length}</strong>개
          </h2>
          {searchedStocks.length > 0 ? (
            <ul>
              {searchedStocks.map((stock) => (
                <li key={stock.stockId}>
                  <button type="button" onClick={() => handleStockSelect(stock)}>
                    <span className={styles['stock-news-page__ticker']}>{stock.ticker}</span>
                    <span className={styles['stock-news-page__names']}>
                      <strong>{stock.koreanName || stock.name}</strong>
                      {stock.koreanName && <small>{stock.name}</small>}
                    </span>
                    {stock.theme && (
                      <span className={styles['stock-news-page__theme']}>{stock.theme}</span>
                    )}
                    <ArrowRight
                      className={styles['stock-news-page__arrow']}
                      aria-hidden="true"
                    ></ArrowRight>
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p>검색된 종목이 없습니다.</p>
          )}
        </section>
      ) : null}

      {errorMessage && (
        <p className={styles['stock-news-page__notice']} role="alert">
          {errorMessage}
        </p>
      )}

      <section
        className={`${styles['stock-news-page__empty']} ${
          isStockSearching || hasStockSearched || selectedStockName
            ? styles['stock-news-page__empty-guide--hidden']
            : ''
        }`}
      >
        <Search aria-hidden="true"></Search>
        <h2>찾고 싶은 종목을 입력해 보세요.</h2>
        <p>종목명을 입력하면 관련 뉴스를 확인할 수 있습니다.</p>
      </section>

      {selectedStockName && (
        <section className={styles['stock-news-page__news']}>
          <section className={styles['stock-news-page__selected-stock']}>
            <span>{selectedTicker || 'SELECTED STOCK'}</span>
            <h2>{selectedStockName || '선택한 종목'}</h2>
            {!isNewsLoading && newsPage && (
              <p>
                뉴스 <strong>{newsPage.totalElements.toLocaleString()}</strong>건
              </p>
            )}
          </section>

          {isNewsLoading ? (
            <ListSkeleton count={5} label="종목 뉴스를 불러오는 중입니다."></ListSkeleton>
          ) : newsPage && newsPage.content.length > 0 ? (
            <>
              <ul className={styles['stock-news-page__list']}>
                {newsPage.content.map((news) => (
                  <li key={news.id}>
                    <article>
                      <time dateTime={news.collectedAt}>{getCollectedDate(news.collectedAt)}</time>
                      <h3>{news.title}</h3>
                      {news.content && <p>{news.content}</p>}
                      <a href={news.url} target="_blank" rel="noreferrer">
                        원문 보기
                        <ExternalLink aria-hidden="true"></ExternalLink>
                      </a>
                    </article>
                  </li>
                ))}
              </ul>

              {newsPage.totalPages > 1 && (
                <nav className={styles['stock-news-page__pagination']} aria-label="종목 뉴스 페이지">
                  <button
                    type="button"
                    disabled={newsPage.first || isNewsLoading}
                    onClick={() => handlePageChange(newsPage.number - 1)}
                  >
                    이전
                  </button>
                  <span>
                    {newsPage.number + 1} / {newsPage.totalPages}
                  </span>
                  <button
                    type="button"
                    disabled={newsPage.last || isNewsLoading}
                    onClick={() => handlePageChange(newsPage.number + 1)}
                  >
                    다음
                  </button>
                </nav>
              )}
            </>
          ) : (
            !errorMessage && (
              <section className={styles['stock-news-page__empty']}>
                <h2>수집된 뉴스가 없습니다.</h2>
                <p>새로운 뉴스가 수집되면 이곳에 표시됩니다.</p>
              </section>
            )
          )}
        </section>
      )}
    </main>
  )
}

export default StockNewsPage
