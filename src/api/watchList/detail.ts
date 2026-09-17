// 관심종목 상세정보 조회
import { apiFetch } from '@/api/common/commonApi'
import { requestWithSessionCache } from '@/utils/requestCache'

export interface WatchListDetail {
  stockId: number
  summary: string | null
  representativeName: string | null
  nation: string | null
  city: string | null
  homepageUrl: string | null
  industryName: string | null
  listedAt: string | null
}

export async function watchListDetail(stockId: number): Promise<WatchListDetail> {
  try {
    const requestPath = `/stocks/detail?stockId=${stockId}`
    const response = await requestWithSessionCache(
      `stock-detail:${requestPath}`,
      () => apiFetch<WatchListDetail>(requestPath),
      300_000,
    )

    if (!response) {
      throw new Error('관심종목 상세정보 응답이 없습니다.')
    }

    console.log('관심종목 상세정보 응답', response)
    return response
  } catch (error) {
    console.log('관심종목 상세정보 에러', error)
    throw error
  }
}
