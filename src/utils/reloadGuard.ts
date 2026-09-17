const RELOAD_HISTORY_KEY = 'stockNewsReloadHistoryV2'
const RELOAD_LIMIT = 3
const RELOAD_WINDOW = 10_000

// 짧은 시간에 반복되는 새로고침을 감지해 안내
export function guardRapidReload() {
  const navigation = performance.getEntriesByType('navigation')[0] as
    | PerformanceNavigationTiming
    | undefined

  if (navigation?.type !== 'reload') {
    sessionStorage.removeItem(RELOAD_HISTORY_KEY)
    return
  }

  const currentLoadTime = performance.timeOrigin
  const savedHistory = sessionStorage.getItem(RELOAD_HISTORY_KEY)
  let reloadHistory: number[] = []

  if (savedHistory) {
    try {
      reloadHistory = JSON.parse(savedHistory) as number[]
    } catch {
      sessionStorage.removeItem(RELOAD_HISTORY_KEY)
    }
  }

  if (reloadHistory.includes(currentLoadTime)) {
    return
  }

  const recentReloads = reloadHistory.filter(
    (reloadTime) => currentLoadTime - reloadTime < RELOAD_WINDOW,
  )

  recentReloads.push(currentLoadTime)
  sessionStorage.setItem(RELOAD_HISTORY_KEY, JSON.stringify(recentReloads))

  if (recentReloads.length === RELOAD_LIMIT) {
    window.alert('연속적으로 새로고침을 할 수 없습니다.')
    sessionStorage.removeItem(RELOAD_HISTORY_KEY)
  }
}
