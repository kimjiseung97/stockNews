// 인기종목 조회
import { apiFetch } from '@/api/common/commonApi'
import { requestWithSessionCache } from '@/utils/requestCache'

export interface PopularList {
  id: number
  ticker: string
  name: string
  theme: string
  koreanName: string
  searchCount: number
}

export async function popularList(limit: number): Promise<PopularList[]> {
  try {
    const requestPath = `/stocks/popular?limit=${limit}`
    const response = await requestWithSessionCache(
      `popular-stock:${requestPath}`,
      () => apiFetch<PopularList[]>(requestPath),
      300_000,
    )

    if (!response) {
      throw new Error('인기종목 조회 응답이 없습니다.')
    }

    console.log('인기종목 조회 응답', response)
    return response
  } catch (error) {
    console.log('인기종목 조회 에러', error)
    throw error
  }
}
