// 이메일 인증

import axios from 'axios'

interface emailAuth {
  email: string
  code: string
}

export async function emailAuth(data: emailAuth): Promise<void> {
  try {
    console.log('보낸 데이터', data)

    const response = await axios.post('/auth/verify', data)

    console.log('인증 성공 응답', response.data)
  } catch (e) {
    console.log('에러', e)
  }
}
