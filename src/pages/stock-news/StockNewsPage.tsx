import { useEffect, useState, type FormEvent } from 'react'
import { ArrowRight, Search } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { ApiError } from '@/api/common/commonApi'
import { stockNews, type StockNewsResponse } from '@/api/stockNews/stockNews'
import { stockSearch, type Stock } from '@/api/stockSearch/stockSearch'
import ListSkeleton from '@/components/common/ListSkeleton'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import styles from '@/assets/styles/pages/stock-news/stockNews.module.scss'
import StockNameBadge from '@/components/common/StockNameBadge'

const PAGE_SIZE = 10

function StockNewsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedStockName = searchParams.get('koreaName') ?? ''
  const [stockId, setStockId] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [selectedTicker, setSelectedTicker] = useState('')
  const [resolvedStockName, setResolvedStockName] = useState('')
  const [keyword, setKeyword] = useState('')
  const [searchedStocks, setSearchedStocks] = useState<Stock[]>([])
  const [newsPage, setNewsPage] = useState<StockNewsResponse | null>(null)
  const [isStockSearching, setIsStockSearching] = useState(false)
  const [hasStockSearched, setHasStockSearched] = useState(false)
  const [isNewsLoading, setIsNewsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    setKeyword(selectedStockName)

    if (!selectedStockName) {
      setStockId(0)
      setSelectedTicker('')
      setResolvedStockName('')
      setCurrentPage(0)
      setNewsPage(null)
      setIsNewsLoading(false)
      return
    }

    let isActive = true

    const loadSelectedStock = async () => {
      setStockId(0)
      setCurrentPage(0)
      setNewsPage(null)
      setIsNewsLoading(true)
      setErrorMessage('')

      try {
        const response = await stockSearch({ keyword: selectedStockName, page: 0, size: 10 })
        const selectedStock = response.content.find(
          (stock) =>
            stock.ticker === selectedStockName ||
            stock.koreanName === selectedStockName ||
            stock.name === selectedStockName,
        )

        if (!isActive) {
          return
        }

        if (!selectedStock) {
          setStockId(0)
          setSelectedTicker('')
          setResolvedStockName('')
          setIsNewsLoading(false)
          setErrorMessage('선택한 종목을 찾지 못했습니다.')
          return
        }

        setStockId(selectedStock.stockId)
        setSelectedTicker(selectedStock.ticker)
        setResolvedStockName(selectedStockName)
      } catch (error) {
        if (!isActive) {
          return
        }

        setStockId(0)
        setSelectedTicker('')
        setResolvedStockName('')
        setIsNewsLoading(false)
        setErrorMessage(
          error instanceof ApiError ? error.message : '선택한 종목을 찾지 못했습니다.',
        )
      }
    }

    void loadSelectedStock()

    return () => {
      isActive = false
    }
  }, [selectedStockName])

  useEffect(() => {
    if (!Number.isInteger(stockId) || stockId <= 0) {
      setNewsPage(null)
      return
    }

    let isActive = true

    const loadNews = async () => {
      setIsNewsLoading(true)
      setErrorMessage('')

      try {
        const response = await stockNews({ stockId, page: currentPage, size: PAGE_SIZE })

        if (isActive) {
          setNewsPage(response)
        }
      } catch (error) {
        if (isActive) {
          setNewsPage(null)
          setErrorMessage(
            error instanceof ApiError ? error.message : '종목 뉴스를 불러오지 못했습니다.',
          )
        }
      } finally {
        if (isActive) {
          setIsNewsLoading(false)
        }
      }
    }

    void loadNews()

    return () => {
      isActive = false
    }
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
    setSearchParams({})
    setStockId(0)
    setSelectedTicker('')
    setCurrentPage(0)
    setNewsPage(null)

    try {
      const response = await stockSearch({ keyword: trimmedKeyword, page: 0, size: 5 })
      setSearchedStocks(response.content)
    } catch (error) {
      setSearchedStocks([])
      setErrorMessage(error instanceof ApiError ? error.message : '종목을 검색하지 못했습니다.')
    } finally {
      setIsStockSearching(false)
    }
  }

  // 선택한 종목의 첫 뉴스 페이지 조회
  const handleStockSelect = (stock: Stock) => {
    setStockId(stock.stockId)
    setSelectedTicker(stock.ticker)
    setResolvedStockName(stock.koreanName || stock.name)
    setCurrentPage(0)
    setNewsPage(null)
    setIsNewsLoading(true)
    setSearchParams({
      koreaName: stock.koreanName || stock.name,
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
    <main id="stockNewsPage" className={styles['stock-news-page']}>
      <hgroup className={styles['stock-news-page__heading']}>
        <h1>종목별 뉴스</h1>
        <p>궁금한 종목을 검색하고 최근 수집된 뉴스를 확인하세요.</p>
      </hgroup>

      <form className={styles['stock-news-page__search-form']} onSubmit={handleStockSearch}>
        <label className={styles['stock-news-page__search-box']}>
          <Search aria-hidden="true"></Search>
          <span className={styles['stock-news-page__sr-only']}>종목 검색</span>
          <input
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
                    <StockNameBadge
                      ticker={stock.ticker}
                      displayName={stock.koreanName || stock.name}
                      secondaryName={stock.koreanName ? stock.name : undefined}
                      theme={stock.theme}
                    ></StockNameBadge>
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
        <hgroup className={styles['stock-news-page__empty-heading']}>
          <h2>찾고 싶은 종목을 입력해 보세요.</h2>
          <Search aria-hidden="true"></Search>
        </hgroup>
        <p>종목명을 입력하면 관련 뉴스를 확인할 수 있습니다.</p>
      </section>

      {selectedStockName &&
      (selectedStockName !== resolvedStockName || (isNewsLoading && !newsPage)) ? (
        <ListSkeleton count={5} label="선택한 종목의 뉴스를 불러오는 중입니다."></ListSkeleton>
      ) : selectedStockName ? (
        <section aria-labelledby="selectedStockTitle">
          <div className={styles['stock-news-page__selected-stock']}>
            <h2 id="selectedStockTitle">
              {selectedStockName || '선택한 종목'}
              <small>{selectedTicker || 'SELECTED STOCK'}</small>
            </h2>
            {!isNewsLoading && newsPage && (
              <span>
                뉴스 <strong>{newsPage.totalElements.toLocaleString()}</strong>건
              </span>
            )}
          </div>

          {isNewsLoading ? (
            <ListSkeleton count={5} label="종목 뉴스를 불러오는 중입니다."></ListSkeleton>
          ) : newsPage && newsPage.content.length > 0 ? (
            <>
              <ul className={styles['stock-news-page__list']}>
                {newsPage.content.map((news) => (
                  <li key={news.id}>
                    <article>
                      <a
                        className={styles['stock-news-page__article-link']}
                        href={news.url}
                        target="_blank"
                        rel="noreferrer"
                      >
                        <h3>{news.title}</h3>
                        <time dateTime={news.collectedAt}>
                          {getCollectedDate(news.collectedAt)}
                        </time>
                        {news.content && <p>{news.content}</p>}
                        <span className={styles['stock-news-page__article-more']}>
                          기사 보기
                          <ArrowRight aria-hidden="true"></ArrowRight>
                        </span>
                      </a>
                    </article>
                  </li>
                ))}
              </ul>

              {newsPage.totalPages > 1 && (
                <nav
                  className={styles['stock-news-page__pagination']}
                  aria-label="종목 뉴스 페이지"
                >
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
                <h3>수집된 뉴스가 없습니다.</h3>
                <p>새로운 뉴스가 수집되면 이곳에 표시됩니다.</p>
              </section>
            )
          )}
        </section>
      ) : null}
    </main>
  )
}

export default StockNewsPage
