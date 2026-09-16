import { createContext, useContext, type RefObject } from 'react'

const ScrollContainerContext = createContext<RefObject<HTMLDivElement | null> | null>(null)

export const ScrollContainerProvider = ScrollContainerContext.Provider

// 실제 페이지 스크롤이 일어나는 MainLayout 우측 패널(overflow-y: auto)의 ref
export function useScrollContainer() {
  return useContext(ScrollContainerContext)
}
