// 관심목록 조회
import { apiFetch } from '@/api/common/commonApi'

export interface WatchListStock {
  id: number
  stockId: number
  ticker: string
  name: string
  theme: string | null
  koreanName: string | null
}

export interface WatchListSearchResponse {
  totalPages: number
  totalElements: number
  size: number
  content: WatchListStock[]
  number: number
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
}

interface WatchListSearchParams {
  page?: number
  size?: number
}

export async function watchListSearch({
  page = 0,
  size = 100,
}: WatchListSearchParams = {}): Promise<WatchListSearchResponse> {
  try {
    const searchParams = new URLSearchParams({ page: String(page), size: String(size) })
    const response = await apiFetch<WatchListSearchResponse>(
      `/users/me/stocks?${searchParams.toString()}`,
    )

    if (!response) {
      throw new Error('관심종목 조회 응답이 없습니다.')
    }

    const normalizedResponse = {
      ...response,
      content: response.content.map((stock) => ({
        ...stock,
        id: stock.id ?? stock.stockId,
        stockId: stock.stockId ?? stock.id,
      })),
    }

    console.log('관심종목 조회 응답', normalizedResponse)
    return normalizedResponse
  } catch (e) {
    console.log('관심종목 조회 에러', e)
    throw e
  }
}
