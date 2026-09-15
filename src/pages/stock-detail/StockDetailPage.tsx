import { useEffect, useState } from 'react'
import { ArrowLeft, ExternalLink } from 'lucide-react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { ApiError } from '@/api/common/commonApi'
import { watchListDetail, type WatchListDetail } from '@/api/watchList/detail'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { useAuth } from '@/contexts/AuthContext'
import styles from '@/assets/styles/pages/stock-detail/stockDetail.module.scss'

interface StockDetailLocationState {
  ticker?: string
  displayName?: string
  name?: string
  returnTo?: string
}

function StockDetailPage() {
  const { email } = useAuth()
  const [searchParams] = useSearchParams()
  const limit = searchParams.get('limit')
  const location = useLocation()
  const stockInformation = location.state as StockDetailLocationState | null
  const [detail, setDetail] = useState<WatchListDetail | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    const loadDetail = async () => {
      const parsedStockId = Number(limit)

      if (!Number.isInteger(parsedStockId) || parsedStockId <= 0) {
        setErrorMessage('올바르지 않은 종목 정보입니다.')
        setIsLoading(false)
        return
      }

      try {
        setDetail(await watchListDetail(parsedStockId))
      } catch (error) {
        setErrorMessage(
          error instanceof ApiError ? error.message : '종목 상세정보를 불러오지 못했습니다.',
        )
      } finally {
        setIsLoading(false)
      }
    }

    void loadDetail()
  }, [limit])

  const getListedDate = (listedAt: string | null) => {
    if (!listedAt) {
      return '-'
    }

    return new Date(listedAt).toLocaleDateString('ko-KR')
  }

  return (
    <main id="stockDetailPage" className={styles['stock-detail-page']}>
      <Link
        className={styles['stock-detail-page__back']}
        to={email ? '/watchlist' : stockInformation?.returnTo || '/stock-search'}
      >
        <ArrowLeft aria-hidden="true"></ArrowLeft>
        {email ? '내 관심종목으로 돌아가기' : '종목 검색으로 돌아가기'}
      </Link>

      {!detail && <h1 className={styles['stock-detail-page__sr-only']}>기업 상세정보</h1>}

      {isLoading ? (
        <p className={styles['stock-detail-page__status']}>
          <LoadingSpinner label="상세정보를 불러오는 중"></LoadingSpinner>
        </p>
      ) : errorMessage ? (
        <p className={styles['stock-detail-page__error']} role="alert">
          {errorMessage}
        </p>
      ) : detail ? (
        <>
          <hgroup className={styles['stock-detail-page__heading']}>
            <span className={styles['stock-detail-page__title-group']}>
              <h1>{stockInformation?.displayName || '기업 상세정보'}</h1>
              <p>{stockInformation?.ticker || `STOCK #${detail.stockId}`}</p>
            </span>
            {stockInformation?.name && <p>{stockInformation.name}</p>}
          </hgroup>

          <section className={styles['stock-detail-page__overview']}>
            <h2>기업 소개</h2>
            <p>{detail.summary || '등록된 기업 소개가 없습니다.'}</p>
          </section>

          <dl className={styles['stock-detail-page__information']}>
            <div>
              <dt>대표자</dt>
              <dd>{detail.representativeName || '-'}</dd>
            </div>
            <div>
              <dt>업종</dt>
              <dd>{detail.industryName || '-'}</dd>
            </div>
            <div>
              <dt>국가</dt>
              <dd>{detail.nation || '-'}</dd>
            </div>
            <div>
              <dt>도시</dt>
              <dd>{detail.city || '-'}</dd>
            </div>
            <div>
              <dt>상장일</dt>
              <dd>{getListedDate(detail.listedAt)}</dd>
            </div>
          </dl>

          {detail.homepageUrl && (
            <a
              className={styles['stock-detail-page__homepage']}
              href={detail.homepageUrl}
              target="_blank"
              rel="noreferrer"
              aria-label="기업 홈페이지 방문"
            >
              기업 홈페이지 방문
              <ExternalLink aria-hidden="true"></ExternalLink>
            </a>
          )}
        </>
      ) : null}
    </main>
  )
}

export default StockDetailPage
