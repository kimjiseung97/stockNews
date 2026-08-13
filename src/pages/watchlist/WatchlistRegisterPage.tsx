import { useEffect, useState, type FormEvent } from 'react'
import { Check, Plus, Search, Trash2 } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { stockSearch, type Stock, type StockSearchResponse } from '@/api/stockSearch/stockSearch'
import { watchListRegist } from '@/api/watchList/regist'
import { watchListRemove } from '@/api/watchList/remove'
import { watchListSearch, type WatchListStock } from '@/api/watchList/search'
import ListSkeleton from '@/components/common/ListSkeleton'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import styles from '@/assets/styles/pages/watchlist/watchlistRegister.module.scss'
import mediaStyles from '@/assets/styles/pages/watchlist/watchlistRegisterMedia.module.scss'

const PAGE_SIZE = 10

function WatchlistRegisterPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const koreaName = searchParams.get('koreaName')?.trim() ?? ''
  const [keyword, setKeyword] = useState(koreaName)
  const [searchedKeyword, setSearchedKeyword] = useState(koreaName)
  const [watchListKeyword, setWatchListKeyword] = useState(koreaName)
  const [searchedWatchListKeyword, setSearchedWatchListKeyword] = useState(koreaName)
  const [watchListCurrentPage, setWatchListCurrentPage] = useState(0)
  const [searchResult, setSearchResult] = useState<StockSearchResponse | null>(null)
  const [watchList, setWatchList] = useState<WatchListStock[]>([])
  const [isSearching, setIsSearching] = useState(true)
  const [isWatchListLoading, setIsWatchListLoading] = useState(true)
  const [processingStockId, setProcessingStockId] = useState<number | null>(null)
  const [message, setMessage] = useState('')

  const loadWatchList = async () => {
    setIsWatchListLoading(true)

    try {
      const response = await watchListSearch()
      console.log('등록된 관심 종목 조회 응답', response)
      setWatchList(response.content)
    } catch {
      setWatchList([])
    } finally {
      setIsWatchListLoading(false)
    }
  }

  const loadStocks = async (searchKeyword: string, page: number) => {
    setIsSearching(true)
    setMessage('')

    try {
      setSearchResult(await stockSearch({ keyword: searchKeyword, page, size: PAGE_SIZE }))
    } catch {
      setSearchResult(null)
    } finally {
      setIsSearching(false)
    }
  }

  useEffect(() => {
    void loadWatchList()
  }, [])

  useEffect(() => {
    setKeyword(koreaName)
    setSearchedKeyword(koreaName)
    setWatchListKeyword(koreaName)
    setSearchedWatchListKeyword(koreaName)
    setWatchListCurrentPage(0)
    void loadStocks(koreaName, 0)
  }, [koreaName])

  // 종목 검색
  const handleSearchSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmedKeyword = keyword.trim()

    setKeyword(trimmedKeyword)
    setSearchedKeyword(trimmedKeyword)

    if (trimmedKeyword !== koreaName) {
      setSearchParams(trimmedKeyword ? { koreaName: trimmedKeyword } : {})
      return
    }

    await loadStocks(trimmedKeyword, 0)
  }

  // 등록된 관심종목 검색
  const handleWatchListSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmedKeyword = watchListKeyword.trim()

    setWatchListKeyword(trimmedKeyword)
    setSearchedWatchListKeyword(trimmedKeyword)
    setWatchListCurrentPage(0)

    if (trimmedKeyword !== koreaName) {
      setSearchParams(trimmedKeyword ? { koreaName: trimmedKeyword } : {})
    }
  }

  // 관심종목 등록
  const handleRegister = async (stock: Stock) => {
    setProcessingStockId(stock.stockId)
    setMessage('')

    try {
      await watchListRegist({ stockIds: [stock.stockId] })
      await loadWatchList()
      setMessage(`${stock.koreanName || stock.name} 종목을 등록했습니다.`)
    } catch {
      setMessage('')
    } finally {
      setProcessingStockId(null)
    }
  }

  // 관심종목 삭제
  const handleRemove = async (stock: WatchListStock) => {
    const stockName = stock.koreanName || stock.name

    if (!window.confirm(`${stockName} 종목을 관심목록에서 삭제할까요?`)) {
      return
    }

    setProcessingStockId(stock.stockId)
    setMessage('')

    try {
      await watchListRemove({ stockIds: [stock.stockId] })
      setWatchList((currentWatchList) =>
        currentWatchList.filter((watchListStock) => watchListStock.stockId !== stock.stockId),
      )
      setMessage(`${stockName} 종목을 삭제했습니다.`)
    } catch {
      setMessage('')
    } finally {
      setProcessingStockId(null)
    }
  }

  const registeredStockIds = new Set(watchList.map((stock) => stock.stockId))
  const normalizedWatchListKeyword = searchedWatchListKeyword.toLowerCase()
  const filteredWatchList = watchList.filter((stock) =>
    [stock.ticker, stock.name, stock.koreanName]
      .filter(Boolean)
      .some((stockName) => stockName?.toLowerCase().includes(normalizedWatchListKeyword)),
  )
  const watchListPageSize = PAGE_SIZE
  const watchListTotalPages = Math.ceil(filteredWatchList.length / watchListPageSize)
  const pagedWatchList = filteredWatchList.slice(
    watchListCurrentPage * watchListPageSize,
    (watchListCurrentPage + 1) * watchListPageSize,
  )

  useEffect(() => {
    if (watchListTotalPages > 0 && watchListCurrentPage >= watchListTotalPages) {
      setWatchListCurrentPage(watchListTotalPages - 1)
    }
  }, [watchListCurrentPage, watchListTotalPages])

  return (
    <main
      id="watchlistRegisterPage"
      className={`${styles['watchlist-register-page']} ${mediaStyles['watchlist-register-page']}`}
    >
      <section className={styles['watchlist-register-page__heading']}>
        <p className={styles['watchlist-register-page__eyebrow']}>MY WATCHLIST</p>
        <h1>관심 종목 관리</h1>
        <p>관심 있는 종목을 검색해 등록하고, 더 이상 필요하지 않은 종목은 삭제하세요.</p>
      </section>

      {message && (
        <p className={styles['watchlist-register-page__notice-success']} role="status">
          <Check aria-hidden="true"></Check>
          {message}
        </p>
      )}

      <section className={styles['watchlist-register-page__contents']}>
        <article className={styles['watchlist-register-page__panel']}>
          <section className={styles['watchlist-register-page__panel-heading']}>
            <h2>관심 종목 검색</h2>
          </section>
          <form
            className={styles['watchlist-register-page__search-form']}
            onSubmit={handleSearchSubmit}
          >
            <label className={styles['watchlist-register-page__search-box']}>
              <Search aria-hidden="true"></Search>
              <span className={styles['watchlist-register-page__sr-only']}>기업명 또는 티커</span>
              <input
                type="search"
                value={keyword}
                placeholder="예: 엔비디아, NVDA"
                maxLength={100}
                autoComplete="off"
                onChange={(event) => {
                  setKeyword(event.target.value)
                }}
              />
            </label>
            <button type="submit" disabled={isSearching}>
              {isSearching ? <LoadingSpinner label="검색 중"></LoadingSpinner> : '검색'}
            </button>
          </form>

          <section className={styles['watchlist-register-page__result']}>
            <h3>
              {searchedKeyword ? '검색 결과' : '전체 종목'}
              {searchResult && <span>{searchResult.totalElements.toLocaleString()}개</span>}
            </h3>
            {isSearching ? (
              <ListSkeleton count={6} label="전체 종목을 불러오는 중입니다."></ListSkeleton>
            ) : searchResult && searchResult.content.length === 0 && !isSearching ? (
              <p className={styles['watchlist-register-page__empty']}>검색 결과가 없습니다.</p>
            ) : searchResult ? (
              <>
                <ul className={styles['watchlist-register-page__list']}>
                  {searchResult.content.map((stock) => {
                    const isRegistered = registeredStockIds.has(stock.stockId)
                    const isProcessing = processingStockId === stock.stockId

                    return (
                      <li key={stock.stockId} className={styles['watchlist-register-page__item']}>
                        <span className={styles['watchlist-register-page__ticker']}>
                          {stock.ticker}
                        </span>
                        <span className={styles['watchlist-register-page__names']}>
                          <strong>{stock.koreanName || stock.name}</strong>
                          {stock.koreanName && <small>{stock.name}</small>}
                        </span>
                        <button
                          type="button"
                          className={styles['watchlist-register-page__register-button']}
                          disabled={isRegistered || isProcessing}
                          onClick={() => void handleRegister(stock)}
                        >
                          {isRegistered ? (
                            <>
                              <Check aria-hidden="true"></Check>
                              등록됨
                            </>
                          ) : isProcessing ? (
                            <LoadingSpinner label="등록 중"></LoadingSpinner>
                          ) : (
                            <>
                              <Plus aria-hidden="true"></Plus>
                              등록
                            </>
                          )}
                        </button>
                      </li>
                    )
                  })}
                </ul>

                {searchResult.totalPages > 1 && (
                  <nav
                    className={styles['watchlist-register-page__pagination']}
                    aria-label="전체 종목 페이지"
                  >
                    <button
                      type="button"
                      disabled={searchResult.first || isSearching}
                      onClick={() => void loadStocks(searchedKeyword, searchResult.number - 1)}
                    >
                      이전
                    </button>
                    <span>
                      {searchResult.number + 1} / {searchResult.totalPages}
                    </span>
                    <button
                      type="button"
                      disabled={searchResult.last || isSearching}
                      onClick={() => void loadStocks(searchedKeyword, searchResult.number + 1)}
                    >
                      다음
                    </button>
                  </nav>
                )}
              </>
            ) : null}
          </section>
        </article>

        <article className={styles['watchlist-register-page__panel']}>
          <section className={styles['watchlist-register-page__panel-heading']}>
            <h2>등록된 관심 종목</h2>
            <strong>{watchList.length}</strong>
          </section>

          <form
            className={styles['watchlist-register-page__search-form']}
            onSubmit={handleWatchListSearchSubmit}
          >
            <label className={styles['watchlist-register-page__search-box']}>
              <Search aria-hidden="true"></Search>
              <span className={styles['watchlist-register-page__sr-only']}>
                등록된 관심종목 기업명 또는 티커
              </span>
              <input
                type="search"
                value={watchListKeyword}
                placeholder="등록된 관심종목 검색"
                maxLength={100}
                autoComplete="off"
                onChange={(event) => {
                  setWatchListKeyword(event.target.value)
                }}
              />
            </label>
            <button type="submit" disabled={isWatchListLoading}>
              검색
            </button>
          </form>

          {isWatchListLoading ? (
            <ListSkeleton count={6} label="등록된 관심종목을 불러오는 중입니다."></ListSkeleton>
          ) : watchList.length === 0 ? (
            <p className={styles['watchlist-register-page__empty']}>
              아직 등록된 관심종목이 없습니다.
            </p>
          ) : filteredWatchList.length === 0 ? (
            <p className={styles['watchlist-register-page__empty']}>검색 결과가 없습니다.</p>
          ) : (
            <>
              <ul className={styles['watchlist-register-page__list']}>
                {pagedWatchList.map((stock) => (
                  <li key={stock.id} className={styles['watchlist-register-page__item']}>
                    <span className={styles['watchlist-register-page__ticker']}>
                      {stock.ticker}
                    </span>
                    <span className={styles['watchlist-register-page__names']}>
                      <strong>{stock.koreanName || stock.name}</strong>
                      {stock.koreanName && <small>{stock.name}</small>}
                    </span>
                    <button
                      type="button"
                      className={styles['watchlist-register-page__remove-button']}
                      disabled={processingStockId === stock.stockId}
                      onClick={() => void handleRemove(stock)}
                      aria-label={`${stock.koreanName || stock.name} 관심종목 삭제`}
                    >
                      <Trash2 aria-hidden="true"></Trash2>
                    </button>
                  </li>
                ))}
              </ul>

              {watchListTotalPages > 1 && (
                <nav
                  className={styles['watchlist-register-page__pagination']}
                  aria-label="등록된 관심종목 페이지"
                >
                  <button
                    type="button"
                    disabled={watchListCurrentPage === 0}
                    onClick={() => setWatchListCurrentPage(watchListCurrentPage - 1)}
                  >
                    이전
                  </button>
                  <span>
                    {watchListCurrentPage + 1} / {watchListTotalPages}
                  </span>
                  <button
                    type="button"
                    disabled={watchListCurrentPage === watchListTotalPages - 1}
                    onClick={() => setWatchListCurrentPage(watchListCurrentPage + 1)}
                  >
                    다음
                  </button>
                </nav>
              )}
            </>
          )}
        </article>
      </section>
    </main>
  )
}

export default WatchlistRegisterPage
