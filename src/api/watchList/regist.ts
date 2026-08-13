// 관심종목 등록
import { apiFetch } from '@/api/common/commonApi'

interface WatchListRegist {
  stockIds: number[]
}

export async function watchListRegist(data: WatchListRegist): Promise<void> {
  try {
    await apiFetch<void>('/users/me/stocks', {
      method: 'POST',
      body: JSON.stringify(data),
    })
    console.log('관심종목 등록 성공')
  } catch (error) {
    console.log('관심종목 등록 에러', error)
    throw error
  }
}
