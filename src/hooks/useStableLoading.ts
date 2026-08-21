import { useEffect, useRef, useState } from 'react'

interface StableLoadingOptions {
  delay?: number
  minimumDuration?: number
}

/**
 * 아주 짧은 요청에는 로딩 UI를 노출하지 않고, 노출된 로딩 UI의 최소 표시 시간 유지
 * 실제 요청 잠금에는 원래 loading 값을 사용하고 화면 표시 여부에만 반환값 사용
 */
export function useStableLoading(
  loading: boolean,
  { delay = 150, minimumDuration = 300 }: StableLoadingOptions = {},
) {
  const [visible, setVisible] = useState(false)
  const visibleSince = useRef<number | null>(null)

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined

    if (loading) {
      timer = setTimeout(() => {
        visibleSince.current = Date.now()
        setVisible(true)
      }, delay)
    } else if (visibleSince.current !== null) {
      const elapsed = Date.now() - visibleSince.current
      const remaining = Math.max(minimumDuration - elapsed, 0)

      timer = setTimeout(() => {
        visibleSince.current = null
        setVisible(false)
      }, remaining)
    } else {
      setVisible(false)
    }

    return () => {
      if (timer) clearTimeout(timer)
    }
  }, [delay, loading, minimumDuration])

  return visible
}
