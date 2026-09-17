// 종목별 뉴스 조회

export interface StockNewsItem {
  id: number
  title: string
  content: string
  url: string
  collectedAt: string
}

export interface StockNewsResponse {
  totalPages: number
  totalElements: number
  size: number
  content: StockNewsItem[]
  number: number
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
}

interface StockNewsParams {
  stockId: number
  page?: number
  size?: number
}

interface StockNewsApiResponse {
  code: string
  message: string
  data: StockNewsResponse | null
}

export async function stockNews({
  stockId,
  page = 0,
  size = 10,
}: StockNewsParams): Promise<StockNewsResponse> {
  try {
    const searchParams = new URLSearchParams({ page: String(page), size: String(size) })
    const response = await fetch(`/stocks/news/${stockId}?${searchParams.toString()}`, {
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
    })
    const body = (await response.json()) as StockNewsResponse | StockNewsApiResponse

    console.log('종목별 뉴스 조회 응답', {
      stockId,
      page,
      size,
      response: body,
    })

    // 원본 페이지 응답과 공통 응답 형식 모두 처리
    if ('content' in body) {
      return body
    }

    // HTTP status보다 본문의 code를 먼저 본다 - 요청 제한(429)처럼 상태 코드가 200이 아닌 실패도
    // 서버가 내려준 안내 메시지를 그대로 보여주기 위함이다.
    if (body.code !== 'OK') {
      throw new Error(body.message)
    }

    if (!response.ok) {
      throw new Error('종목 뉴스를 불러오지 못했습니다.')
    }

    if (!body.data) {
      throw new Error('종목 뉴스 응답이 없습니다.')
    }

    return body.data
  } catch (error) {
    console.error('종목별 뉴스 조회 오류', {
      stockId,
      page,
      size,
      error,
    })
    throw error
  }
}
