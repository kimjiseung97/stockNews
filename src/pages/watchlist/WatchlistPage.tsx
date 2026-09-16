import { useEffect, useState, type FormEvent } from 'react'
import { ArrowRight, Search } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError } from '@/api/common/commonApi'
import {
  watchListSearch,
  type WatchListSearchResponse,
  type WatchListStock,
} from '@/api/watchList/search'
import ListSkeleton from '@/components/common/ListSkeleton'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import styles from '@/assets/styles/pages/watchlist/watchlist.module.scss'
import mediaStyles from '@/assets/styles/pages/watchlist/watchlistMedia.module.scss'
import { useWatchlistStore } from '@/store/watchlistStore'
import StockNameBadge from '@/components/common/StockNameBadge'

const PAGE_SIZE = 10

function WatchlistPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const koreaName = searchParams.get('koreaName')?.trim() ?? ''
  const [keyword, setKeyword] = useState(koreaName)
  const [searchedKeyword, setSearchedKeyword] = useState(koreaName)
  const [watchList, setWatchList] = useState<WatchListStock[]>([])
  const [watchListPage, setWatchListPage] = useState<WatchListSearchResponse | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const fetchWatchList = useWatchlistStore((state) => state.fetchWatchList)

  useEffect(() => {
    const loadWatchList = async () => {
      try {
        setIsLoading(true)

        if (!searchedKeyword) {
          const response = await watchListSearch({ page: currentPage, size: PAGE_SIZE })
          setWatchList(response.content)
          setWatchListPage(response)
          return
        }

        const allWatchList = await fetchWatchList()
        const normalizedKeyword = searchedKeyword.toLowerCase()
        const searchedWatchList = allWatchList.filter((stock) =>
          [stock.ticker, stock.name, stock.koreanName]
            .filter(Boolean)
            .some((stockName) => stockName?.toLowerCase().includes(normalizedKeyword)),
        )
        const totalPages = Math.ceil(searchedWatchList.length / PAGE_SIZE)
        const pagedWatchList = searchedWatchList.slice(
          currentPage * PAGE_SIZE,
          (currentPage + 1) * PAGE_SIZE,
        )

        setWatchList(pagedWatchList)
        setWatchListPage({
          content: pagedWatchList,
          totalPages,
          totalElements: searchedWatchList.length,
          size: PAGE_SIZE,
          number: currentPage,
          numberOfElements: pagedWatchList.length,
          first: currentPage === 0,
          last: totalPages === 0 || currentPage === totalPages - 1,
          empty: pagedWatchList.length === 0,
        })
      } catch (error) {
        setErrorMessage(
          error instanceof ApiError ? error.message : '관심종목을 불러오지 못했습니다.',
        )
      } finally {
        setIsLoading(false)
      }
    }

    void loadWatchList()
  }, [currentPage, searchedKeyword, fetchWatchList])

  useEffect(() => {
    setKeyword(koreaName)
    setSearchedKeyword(koreaName)
    setCurrentPage(0)
  }, [koreaName])

  // 등록된 관심종목 검색
  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmedKeyword = keyword.trim()

    setKeyword(trimmedKeyword)
    setSearchedKeyword(trimmedKeyword)
    setCurrentPage(0)

    if (trimmedKeyword !== koreaName) {
      setSearchParams(trimmedKeyword ? { koreaName: trimmedKeyword } : {})
    }
  }

  const normalizedKeyword = searchedKeyword.toLowerCase()
  const filteredWatchList = watchList.filter((stock) =>
    [stock.ticker, stock.name, stock.koreanName]
      .filter(Boolean)
      .some((stockName) => stockName?.toLowerCase().includes(normalizedKeyword)),
  )

  return (
    <main
      id="watchlistPage"
      className={`${styles['watchlist-page']} ${mediaStyles['watchlist-page']}`}
    >
      <hgroup className={styles['watchlist-page__heading']}>
        <h1>내 관심종목</h1>
        <p className={styles['watchlist-page__heading-description']}>
          관심종목을 선택해 기업 소개와 기본정보를 확인하세요.
        </p>
      </hgroup>

      <form className={styles['watchlist-page__search-form']} onSubmit={handleSearchSubmit}>
        <label className={styles['watchlist-page__search-box']}>
          <Search aria-hidden="true"></Search>
          <span className={styles['watchlist-page__sr-only']}>기업명 또는 티커</span>
          <input
            value={keyword}
            placeholder="관심종목에서 검색"
            maxLength={100}
            autoComplete="off"
            onChange={(event) => setKeyword(event.target.value)}
          ></input>
        </label>
        <button className={styles['watchlist-page__search-button']} type="submit" disabled={isLoading}>
          {isLoading ? <LoadingSpinner label="조회 중"></LoadingSpinner> : '검색'}
        </button>
      </form>

      {errorMessage && (
        <p className={styles['watchlist-page__notice']} role="alert">
          {errorMessage}
        </p>
      )}

      {isLoading ? (
        <ListSkeleton count={6} label="내 관심종목을 불러오는 중입니다."></ListSkeleton>
      ) : watchList.length === 0 && !searchedKeyword ? (
        <div className={styles['watchlist-page__empty']}>
          <h2>아직 관심종목이 없습니다.</h2>
          <p>관심종목 추가·관리 메뉴에서 종목을 먼저 등록해 주세요.</p>
          <Link to="/watchlist/register">관심종목 추가하기</Link>
        </div>
      ) : filteredWatchList.length === 0 ? (
        <p className={styles['watchlist-page__status']} role="status">
          “{searchedKeyword}”에 해당하는 관심종목이 없습니다.
        </p>
      ) : (
        <div aria-labelledby="watchlistResultTitle">
          <h2
            id="watchlistResultTitle"
            className={styles['watchlist-page__summary']}
            aria-live="polite"
          >
            관심종목 <strong>{watchListPage?.totalElements ?? filteredWatchList.length}</strong>개
          </h2>
          <ul className={styles['watchlist-page__list']}>
            {filteredWatchList.map((stock) => (
              <li key={stock.id}>
                <Link
                  className={`${styles['watchlist-page__item']} ${mediaStyles['watchlist-page__item']}`}
                  to={`/stocks/detail?limit=${stock.stockId}`}
                  state={{
                    ticker: stock.ticker,
                    displayName: stock.koreanName || stock.name,
                    name: stock.koreanName ? stock.name : '',
                  }}
                >
                  <StockNameBadge
                    ticker={stock.ticker}
                    displayName={stock.koreanName || stock.name}
                    secondaryName={stock.koreanName ? stock.name : undefined}
                    theme={stock.theme}
                    themeClassName={mediaStyles['watchlist-page__theme']}
                  ></StockNameBadge>
                  <ArrowRight
                    className={styles['watchlist-page__arrow']}
                    aria-hidden="true"
                  ></ArrowRight>
                </Link>
              </li>
            ))}
          </ul>
          {watchListPage && watchListPage.totalPages > 1 && (
            <nav className={styles['watchlist-page__pagination']} aria-label="내 관심종목 페이지">
              <button
                type="button"
                disabled={watchListPage.first || isLoading}
                onClick={() => setCurrentPage(watchListPage.number - 1)}
              >
                이전
              </button>
              <span>
                {watchListPage.number + 1} / {watchListPage.totalPages}
              </span>
              <button
                type="button"
                disabled={watchListPage.last || isLoading}
                onClick={() => setCurrentPage(watchListPage.number + 1)}
              >
                다음
              </button>
            </nav>
          )}
        </div>
      )}
    </main>
  )
}

export default WatchlistPage
