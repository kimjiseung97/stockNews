// 회원가입 API
import { apiFetch } from '@/api/common/commonApi'

interface signUp {
  email: string
  password: string
  recoveryEmail: string
}

interface SignUpResponse {
  isMailSendSuccess: boolean
}

export async function signUp(data: signUp): Promise<SignUpResponse> {
  try {
    console.log('보낸 데이터', data)

    const response = await apiFetch<SignUpResponse>('/auth/signup', {
      method: 'POST',
      body: JSON.stringify(data),
    })
    console.log('회원가입 응답', response)

    if (!response) {
      throw new Error('회원가입 응답이 없습니다.')
    }

    return response
  } catch (e) {
    console.log('에러', e)
    throw e
  }
}
