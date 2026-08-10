// 아이디 찾기
import { apiFetch } from '@/api/common/commonApi'

interface findId {
  recoveryEmail: string
}

// 아이디 재설정 인증 코드 요청
export async function requestResetPassword(data: findId): Promise<void> {
  try {
    console.log('아이디 찾기 인증 코드 요청 데이터', data)

    const response = await apiFetch('/auth/find-email/request', {
      method: 'POST',
      body: JSON.stringify(data),
    })

    console.log('아이디 찾기 인증 코드 발송 성공 응답', response)
  } catch (error) {
    console.log('아이디 찾기 인증 코드 발송 에러', error)
    throw error
  }
}

//  아이디 재설정 인증
