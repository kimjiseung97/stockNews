// 로그인 API 요청
import { apiFetch } from '@/api/common/commonApi'

interface LoginRequest {
  email: string
  password: string
}

interface LoginResponse {
  email: string
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  try {
    console.log('보낸 데이터', data)

    const response = await apiFetch<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    })

    console.log('로그인 성공 응답', response)
    if (!response) {
      throw new Error('로그인 응답에 사용자 정보가 없습니다.')
    }

    return response
  } catch (e) {
    console.log('로그인 에러', e)
    throw e
  }
}

export async function logout(): Promise<void> {
  await apiFetch('/auth/logout', { method: 'POST' })
}
