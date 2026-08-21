// 공통 API 요청
interface ApiResponse<T> {
  code: string
  message: string
  data: T | null
}

export class ApiError extends Error {
  code: string

  constructor(code: string, message: string) {
    super(message)
    this.code = code
  }
}

// 백엔드 ApiResponse 포맷 파싱 및 오류 처리
export async function apiFetch<T = null>(path: string, options: RequestInit = {}): Promise<T | null> {
  const response = await fetch(path, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  })

  const body = (await response.json()) as ApiResponse<T>

  if (body.code !== 'OK') {
    throw new ApiError(body.code, body.message)
  }

  return body.data
}
