// 관심종목 해제
import { apiFetch } from '@/api/common/commonApi'

interface WatchListRemove {
  stockIds: number[]
}

export async function watchListRemove(data: WatchListRemove): Promise<void> {
  try {
    await apiFetch<void>('/users/me/stocks', {
      method: 'DELETE',
      body: JSON.stringify(data),
    })
    console.log('관심종목 해제 성공')
  } catch (error) {
    console.log('관심종목 해제 에러', error)
    throw error
  }
}
