// 종목 검색
import { apiFetch } from '@/api/common/commonApi'

export interface Stock {
  id: number
  stockId: number
  ticker: string
  name: string
  theme: string | null
  koreanName: string | null
}

export interface StockSort {
  empty: boolean
  unsorted: boolean
  sorted: boolean
}

export interface StockPageable {
  offset: number
  sort: StockSort
  unpaged: boolean
  paged: boolean
  pageNumber: number
  pageSize: number
}

export interface StockSearchResponse {
  totalPages: number
  totalElements: number
  size: number
  content: Stock[]
  number: number
  sort: StockSort
  pageable: StockPageable
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface StockSearchParams {
  keyword?: string
  page?: number
  size?: number
}

export async function stockSearch({
  keyword = '',
  page = 0,
  size = 10,
}: StockSearchParams = {}): Promise<StockSearchResponse> {
  try {
    const searchParams = new URLSearchParams({ page: String(page), size: String(size) })

    if (keyword.trim()) searchParams.set('keyword', keyword.trim())

    const response = await apiFetch<StockSearchResponse>(`/stocks?${searchParams.toString()}`)

    if (!response) {
      throw new Error('종목 검색 응답이 없습니다.')
    }

    console.log('종목 검색 응답', {
      keyword: keyword.trim(),
      page,
      size,
      response,
    })

    return {
      ...response,
      content: response.content.map((stock) => ({
        ...stock,
        stockId: stock.stockId ?? stock.id,
      })),
    }
  } catch (error) {
    console.error('종목 검색 오류', error)
    throw error
  }
}
