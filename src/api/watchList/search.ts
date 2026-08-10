//  관심목록 조회
import { apiFetch } from '@/api/common/commonApi'

interface watchListSearch {
  duplicated: boolean
  isMailSendSuccess: boolean
}

export async function watchListSearch(): Promise<void> {
  try {
    const response = await apiFetch<watchListSearch>(`/users/me/stocks`)

    console.log('이메일 중복 확인 응답', response)

    if (!response) {
      throw new Error('이메일 중복 확인 응답이 없습니다.')
    }
  } catch (e) {
    console.log('이메일 중복 확인 에러', e)
    throw e
  }
}
