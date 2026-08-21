// 아이디 찾기 API 관리
import { apiFetch } from '@/api/common/commonApi'

interface findId {
  recoveryEmail: string
}
interface requsetNumAuth {
  recoveryEmail: string
  code: string
}

interface FindEmailResponse {
  email: string
}

// 아이디 찾기 인증 코드 요청
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

// 아이디 인증 코드 확인

export async function requsetNumAuth(data: requsetNumAuth): Promise<FindEmailResponse | null> {
  try {
    console.log('아이디 찾기 인증 확인 요청 데이터', data)
    const respons = await apiFetch<FindEmailResponse>('/auth/find-email/verify', {
      method: 'POST',
      body: JSON.stringify(data),
    })
    console.log('아이디 찾기 인증 확인 코드 성공 응답', respons)
    return respons
  } catch (error) {
    console.log('아이디 찾기 인증 확인 에러', error)
    throw error
  }
}
