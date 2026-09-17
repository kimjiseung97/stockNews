interface CachedValue<T> {
  expiresAt: number
  value: T
}

const pendingRequests = new Map<string, Promise<unknown>>()

// 같은 조회 요청을 합치고 새로고침 시 짧은 시간 동안 결과를 재사용
export async function requestWithSessionCache<T>(
  key: string,
  request: () => Promise<T>,
  cacheTime: number,
): Promise<T> {
  const cachedValue = sessionStorage.getItem(key)

  if (cachedValue) {
    try {
      const cached = JSON.parse(cachedValue) as CachedValue<T>

      if (cached.expiresAt > Date.now()) {
        return cached.value
      }

      sessionStorage.removeItem(key)
    } catch {
      sessionStorage.removeItem(key)
    }
  }

  const pendingRequest = pendingRequests.get(key) as Promise<T> | undefined

  if (pendingRequest) {
    return pendingRequest
  }

  const nextRequest = request()
    .then((value) => {
      sessionStorage.setItem(
        key,
        JSON.stringify({
          expiresAt: Date.now() + cacheTime,
          value,
        }),
      )

      return value
    })
    .finally(() => pendingRequests.delete(key))

  pendingRequests.set(key, nextRequest)
  return nextRequest
}
