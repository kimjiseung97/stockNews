// 비밀번호 찾기
import { apiFetch } from '@/api/common/commonApi'

interface ResetPasswordRequest {
  email: string
}
interface RequsetNumAuth {
  email: string
  code: string
}
// 비밀번호 재설정 인증 코드 요청
export async function requestResetPassword(data: ResetPasswordRequest): Promise<boolean> {
  try {
    console.log('비밀번호 재설정 요청 데이터', data)

    const response = await apiFetch<boolean>('/auth/reset-password/request', {
      method: 'POST',
      body: JSON.stringify(data),
    })

    console.log('비밀번호 재설정 인증 코드 발송 성공 응답', response)
    return response === true
  } catch (error) {
    console.log('비밀번호 재설정 인증 코드 발송 에러', error)
    throw error
  }
}

//  인증 코드
export async function requsetNumAuth(data: RequsetNumAuth): Promise<boolean> {
  try {
    console.log('비밀번호 재설정 인증코드 데이터', data)
    const response = await apiFetch<boolean>('/auth/reset-password/confirm', {
      method: 'POST',
      body: JSON.stringify(data),
    })
    console.log('비밀번호 재설정 인증코드 발송 성공 응답', response)
    return response === true
  } catch (error) {
    console.log('비밀번호 재설정 인증코드 에러', error)
    throw error
  }
}
