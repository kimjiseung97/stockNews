import { useEffect, useState, type FormEvent } from 'react'
import { Check, Plus, Search, Trash2 } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { stockSearch, type Stock, type StockSearchResponse } from '@/api/stockSearch/stockSearch'
import { watchListRegist } from '@/api/watchList/regist'
import { watchListRemove } from '@/api/watchList/remove'
import { watchListSearch, type WatchListStock } from '@/api/watchList/search'
import ListSkeleton from '@/components/common/ListSkeleton'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import completeIcon from '@/assets/images/icons/complete.png'
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
  const [selectedSearchStockIds, setSelectedSearchStockIds] = useState<number[]>([])
  const [selectedWatchListStockIds, setSelectedWatchListStockIds] = useState<number[]>([])
  const [isBulkProcessing, setIsBulkProcessing] = useState(false)
  const [message, setMessage] = useState('')

  const loadWatchList = async () => {
    setIsWatchListLoading(true)

    try {
      const response = await watchListSearch()
      console.log('등록된 관심 종목 조회 응답', response)

      if (response.totalPages <= 1) {
        setWatchList(response.content)
        return
      }

      const remainingResponses = await Promise.all(
        Array.from({ length: response.totalPages - 1 }, (_, index) =>
          watchListSearch({ page: index + 1, size: response.size }),
        ),
      )
      setWatchList([
        ...response.content,
        ...remainingResponses.flatMap((remainingResponse) => remainingResponse.content),
      ])
    } catch {
      setMessage('관심종목 목록을 불러오지 못했습니다.')
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
    setSelectedSearchStockIds([])
    void loadStocks(koreaName, 0)
  }, [koreaName])

  // 종목 검색
  const handleSearchSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmedKeyword = keyword.trim()

    setKeyword(trimmedKeyword)
    setSearchedKeyword(trimmedKeyword)
    setSelectedSearchStockIds([])

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
      setWatchList((currentWatchList) =>
        currentWatchList.some((watchListStock) => watchListStock.stockId === stock.stockId)
          ? currentWatchList
          : [...currentWatchList, { ...stock, id: stock.stockId }],
      )
      setWatchListKeyword('')
      setSearchedWatchListKeyword('')
      setWatchListCurrentPage(0)
      await loadWatchList()
      setSelectedSearchStockIds((currentStockIds) =>
        currentStockIds.filter((stockId) => stockId !== stock.stockId),
      )
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
      setSelectedWatchListStockIds((currentStockIds) =>
        currentStockIds.filter((stockId) => stockId !== stock.stockId),
      )
      setMessage(`${stockName} 종목을 삭제했습니다.`)
    } catch {
      setMessage('')
    } finally {
      setProcessingStockId(null)
    }
  }

  // 검색 결과 선택
  const handleSearchStockSelect = (stockId: number) => {
    setSelectedSearchStockIds((currentStockIds) =>
      currentStockIds.includes(stockId)
        ? currentStockIds.filter((currentStockId) => currentStockId !== stockId)
        : [...currentStockIds, stockId],
    )
  }

  // 검색 결과 선택 등록
  const handleSelectedRegister = async () => {
    const stockIds = selectedSearchStockIds.filter((stockId) => !registeredStockIds.has(stockId))
    const selectedStocks =
      searchResult?.content.filter((stock) => stockIds.includes(stock.stockId)) ?? []

    if (stockIds.length === 0) {
      return
    }

    setIsBulkProcessing(true)
    setMessage('')

    try {
      await watchListRegist({ stockIds })
      setWatchList((currentWatchList) => {
        const currentStockIds = new Set(currentWatchList.map((stock) => stock.stockId))
        const newStocks = selectedStocks
          .filter((stock) => !currentStockIds.has(stock.stockId))
          .map((stock) => ({ ...stock, id: stock.stockId }))

        return [...currentWatchList, ...newStocks]
      })
      setWatchListKeyword('')
      setSearchedWatchListKeyword('')
      setWatchListCurrentPage(0)
      await loadWatchList()
      setSelectedSearchStockIds([])
      setMessage(`선택한 ${stockIds.length}개 종목을 등록했습니다.`)
    } catch {
      setMessage('')
    } finally {
      setIsBulkProcessing(false)
    }
  }

  // 등록된 관심종목 선택
  const handleWatchListStockSelect = (stockId: number) => {
    setSelectedWatchListStockIds((currentStockIds) =>
      currentStockIds.includes(stockId)
        ? currentStockIds.filter((currentStockId) => currentStockId !== stockId)
        : [...currentStockIds, stockId],
    )
  }

  // 선택한 관심종목 삭제
  const handleSelectedRemove = async () => {
    if (
      selectedWatchListStockIds.length === 0 ||
      !window.confirm(`선택한 ${selectedWatchListStockIds.length}개 종목을 삭제할까요?`)
    ) {
      return
    }

    setIsBulkProcessing(true)
    setMessage('')

    try {
      await watchListRemove({ stockIds: selectedWatchListStockIds })
      setWatchList((currentWatchList) =>
        currentWatchList.filter(
          (stock) => !selectedWatchListStockIds.includes(stock.stockId),
        ),
      )
      setMessage(`선택한 ${selectedWatchListStockIds.length}개 종목을 삭제했습니다.`)
      setSelectedWatchListStockIds([])
    } catch {
      setMessage('')
    } finally {
      setIsBulkProcessing(false)
    }
  }

  // 등록된 관심종목 모두 삭제
  const handleAllRemove = async () => {
    const stockIds = watchList.map((stock) => stock.stockId)

    if (
      stockIds.length === 0 ||
      !window.confirm(`등록된 관심종목 ${stockIds.length}개를 모두 삭제할까요?`)
    ) {
      return
    }

    setIsBulkProcessing(true)
    setMessage('')

    try {
      await watchListRemove({ stockIds })
      setWatchList([])
      setSelectedWatchListStockIds([])
      setMessage(`등록된 관심종목 ${stockIds.length}개를 모두 삭제했습니다.`)
    } catch {
      setMessage('')
    } finally {
      setIsBulkProcessing(false)
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
  const selectableSearchStockIds =
    searchResult?.content
      .filter((stock) => !registeredStockIds.has(stock.stockId))
      .map((stock) => stock.stockId) ?? []
  const isAllSearchStocksSelected =
    selectableSearchStockIds.length > 0 &&
    selectableSearchStockIds.every((stockId) => selectedSearchStockIds.includes(stockId))
  const pagedWatchListStockIds = pagedWatchList.map((stock) => stock.stockId)
  const isAllPagedWatchListSelected =
    pagedWatchListStockIds.length > 0 &&
    pagedWatchListStockIds.every((stockId) => selectedWatchListStockIds.includes(stockId))

  // 현재 검색 결과를 모두 선택
  const handleAllSearchStocksSelect = () => {
    setSelectedSearchStockIds((currentStockIds) =>
      isAllSearchStocksSelected
        ? currentStockIds.filter((stockId) => !selectableSearchStockIds.includes(stockId))
        : [...new Set([...currentStockIds, ...selectableSearchStockIds])],
    )
  }

  // 현재 등록 목록을 모두 선택
  const handleAllPagedWatchListSelect = () => {
    setSelectedWatchListStockIds((currentStockIds) =>
      isAllPagedWatchListSelected
        ? currentStockIds.filter((stockId) => !pagedWatchListStockIds.includes(stockId))
        : [...new Set([...currentStockIds, ...pagedWatchListStockIds])],
    )
  }

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
          <img src={completeIcon} alt=""></img>
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
                <section className={styles['watchlist-register-page__list-actions']}>
                  <label className={styles['watchlist-register-page__select-all']}>
                    <input
                      type="checkbox"
                      checked={isAllSearchStocksSelected}
                      disabled={selectableSearchStockIds.length === 0 || isBulkProcessing}
                      onChange={handleAllSearchStocksSelect}
                    />
                    현재 목록 모두 선택
                  </label>
                  <button
                    type="button"
                    className={styles['watchlist-register-page__selected-register-button']}
                    disabled={selectedSearchStockIds.length === 0 || isBulkProcessing}
                    onClick={() => void handleSelectedRegister()}
                  >
                    {isBulkProcessing ? (
                      <LoadingSpinner label="처리 중"></LoadingSpinner>
                    ) : (
                      `선택 등록 (${selectedSearchStockIds.length})`
                    )}
                  </button>
                </section>
                <ul className={styles['watchlist-register-page__list']}>
                  {searchResult.content.map((stock) => {
                    const isRegistered = registeredStockIds.has(stock.stockId)
                    const isProcessing = processingStockId === stock.stockId

                    return (
                      <li key={stock.stockId} className={styles['watchlist-register-page__item']}>
                        <label className={styles['watchlist-register-page__checkbox']}>
                          <input
                            type="checkbox"
                            checked={selectedSearchStockIds.includes(stock.stockId)}
                            disabled={isRegistered || isProcessing || isBulkProcessing}
                            onChange={() => handleSearchStockSelect(stock.stockId)}
                          />
                          <span className={styles['watchlist-register-page__sr-only']}>
                            {stock.koreanName || stock.name} 선택
                          </span>
                        </label>
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
                          disabled={isRegistered || isProcessing || isBulkProcessing}
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
              <section className={styles['watchlist-register-page__list-actions']}>
                <label className={styles['watchlist-register-page__select-all']}>
                  <input
                    type="checkbox"
                    checked={isAllPagedWatchListSelected}
                    disabled={isBulkProcessing}
                    onChange={handleAllPagedWatchListSelect}
                  />
                  현재 목록 모두 선택
                </label>
                <span className={styles['watchlist-register-page__action-buttons']}>
                  <button
                    type="button"
                    className={styles['watchlist-register-page__selected-remove-button']}
                    disabled={selectedWatchListStockIds.length === 0 || isBulkProcessing}
                    onClick={() => void handleSelectedRemove()}
                  >
                    선택 삭제 ({selectedWatchListStockIds.length})
                  </button>
                  <button
                    type="button"
                    className={styles['watchlist-register-page__all-remove-button']}
                    disabled={isBulkProcessing}
                    onClick={() => void handleAllRemove()}
                  >
                    모두 삭제
                  </button>
                </span>
              </section>
              <ul className={styles['watchlist-register-page__list']}>
                {pagedWatchList.map((stock) => (
                  <li key={stock.id} className={styles['watchlist-register-page__item']}>
                    <label className={styles['watchlist-register-page__checkbox']}>
                      <input
                        type="checkbox"
                        checked={selectedWatchListStockIds.includes(stock.stockId)}
                        disabled={isBulkProcessing}
                        onChange={() => handleWatchListStockSelect(stock.stockId)}
                      />
                      <span className={styles['watchlist-register-page__sr-only']}>
                        {stock.koreanName || stock.name} 선택
                      </span>
                    </label>
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
                      disabled={processingStockId === stock.stockId || isBulkProcessing}
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
