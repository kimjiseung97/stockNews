// 이메일 인증 API 요청

import { apiFetch } from '@/api/common/commonApi'

interface emailAuth {
  email: string
  code: string
}

export async function emailAuth(data: emailAuth): Promise<void> {
  try {
    console.log('보낸 데이터', data)

    const response = await apiFetch('/auth/verify', {
      method: 'POST',
      body: JSON.stringify(data),
    })

    console.log('인증 성공 응답', response)
  } catch (e) {
    console.log('에러', e)
    throw e
  }
}
