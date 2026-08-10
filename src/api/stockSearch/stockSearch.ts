// 종목 검색
import { apiFetch } from '@/api/common/commonApi'

interface stockSearch {
  duplicated: boolean
  isMailSendSuccess: boolean
}

export async function stockSearch(): Promise<void> {
  try {
    const response = await apiFetch<stockSearch>(`/users/me/stocks`)

    console.log('이메일 중복 확인 응답', response)

    if (!response) {
      throw new Error('이메일 중복 확인 응답이 없습니다.')
    }
  } catch (e) {
    console.log('이메일 중복 확인 에러', e)
    throw e
  }
}
