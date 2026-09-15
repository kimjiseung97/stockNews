import { create } from 'zustand'
import { fetchAllWatchListStocks, type WatchListStock } from '@/api/watchList/search'

type StocksUpdater = WatchListStock[] | ((prevStocks: WatchListStock[]) => WatchListStock[])

interface WatchlistStoreState {
  stocks: WatchListStock[]
  status: 'idle' | 'loading' | 'loaded'
  setStocks: (updater: StocksUpdater) => void
  // 캐시가 있으면 재사용, 없으면 새로 조회
  fetchWatchList: () => Promise<WatchListStock[]>
  // 캐시 무시하고 서버에서 새로 조회
  refreshWatchList: () => Promise<WatchListStock[]>
}

export const useWatchlistStore = create<WatchlistStoreState>((set, get) => ({
  stocks: [],
  status: 'idle',
  setStocks: (updater) =>
    set((state) => ({
      stocks: typeof updater === 'function' ? updater(state.stocks) : updater,
    })),
  fetchWatchList: async () => {
    const { status, stocks } = get()

    if (status === 'loaded') {
      return stocks
    }

    set({ status: 'loading' })
    const nextStocks = await fetchAllWatchListStocks()
    set({ stocks: nextStocks, status: 'loaded' })
    return nextStocks
  },
  refreshWatchList: async () => {
    const nextStocks = await fetchAllWatchListStocks()
    set({ stocks: nextStocks, status: 'loaded' })
    return nextStocks
  },
}))
