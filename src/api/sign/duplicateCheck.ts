// 이메일 중복여부 API
import { apiFetch } from '@/api/common/commonApi'

interface duplicateCheck {
  duplicated: boolean
  isMailSendSuccess: boolean
}

export async function duplicateCheck(email: string): Promise<duplicateCheck> {
  try {
    const params = new URLSearchParams({ email: email })
    const response = await apiFetch<duplicateCheck>(`/auth/email/exists?${params.toString()}`)

    console.log('이메일 중복 확인 응답', response)

    if (!response) {
      throw new Error('이메일 중복 확인 응답이 없습니다.')
    }

    return response
  } catch (e) {
    console.log('이메일 중복 확인 에러', e)
    throw e
  }
}
