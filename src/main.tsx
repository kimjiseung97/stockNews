import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@/assets/styles/common/reset.scss'
import '@/assets/styles/common/common.scss'
import App from './App.tsx'
import { guardRapidReload } from '@/utils/reloadGuard'

guardRapidReload()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App></App>
    </BrowserRouter>
  </StrictMode>,
)
