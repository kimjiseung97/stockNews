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

const pendingApiRequests = new Map<string, Promise<unknown>>()

// 백엔드 ApiResponse 포맷 파싱 및 오류 처리
export async function apiFetch<T = null>(
  path: string,
  options: RequestInit = {},
): Promise<T | null> {
  const requestKey = `${options.method ?? 'GET'}:${path}:${String(options.body ?? '')}`
  const pendingRequest = pendingApiRequests.get(requestKey) as Promise<T | null> | undefined

  if (pendingRequest) {
    return pendingRequest
  }

  const request = async () => {
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

  const requestPromise = request().finally(() => pendingApiRequests.delete(requestKey))
  pendingApiRequests.set(requestKey, requestPromise)

  return requestPromise
}
