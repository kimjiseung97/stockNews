import { apiFetch } from '@/api/common/commonApi'

interface ChatResponse {
  answer?: string
  message?: string
  response?: string
  content?: string
}

// 질문을 보내고 답변 문구를 반환
export async function askStockChat(question: string): Promise<string> {
  const requestBody = { question }

  console.log('주식 채팅 요청', {
    url: '/stocks/chat',
    method: 'POST',
    body: requestBody,
  })

  const response = await apiFetch<string | ChatResponse>('/stocks/chat', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })

  if (typeof response === 'string') return response

  const answer = response?.answer ?? response?.message ?? response?.response ?? response?.content

  if (!answer) {
    throw new Error('답변을 불러오지 못했습니다.')
  }

  return answer
}
