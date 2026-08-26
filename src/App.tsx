import { lazy } from 'react'
import { Route, Routes } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import { AuthProvider } from './contexts/AuthContext'
import EmailSettingsPage from './pages/email-settings/EmailSettingsPage'

// 필요한 화면 코드만 불러오기
const HomePage = lazy(() => import('./pages/home/HomePage'))
const SignUpPage = lazy(() => import('./pages/sign-up/SignUpPage'))
const StockNewsPage = lazy(() => import('./pages/stock-news/StockNewsPage'))
const StockDetailPage = lazy(() => import('./pages/stock-detail/StockDetailPage'))
const StockSearchPage = lazy(() => import('./pages/stock-search/StockSearchPage'))
const WatchlistRegisterPage = lazy(() => import('./pages/watchlist/WatchlistRegisterPage'))
const WatchlistPage = lazy(() => import('./pages/watchlist/WatchlistPage'))
const LoginPage = lazy(() => import('./pages/login/LoginPage'))
const FindPasswordPage = lazy(() => import('./pages/find-password/FindPasswordPage'))
const FindEmailPage = lazy(() => import('./pages/find-email/FindEmailPage'))

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<MainLayout></MainLayout>}>
          <Route path="/" element={<HomePage></HomePage>}></Route>
          <Route path="/stock-search" element={<StockSearchPage></StockSearchPage>}></Route>
          <Route path="/watchlist" element={<WatchlistPage></WatchlistPage>}></Route>
          <Route path="/stocks/detail" element={<StockDetailPage></StockDetailPage>}></Route>
          <Route
            path="/watchlist/register"
            element={<WatchlistRegisterPage></WatchlistRegisterPage>}
          ></Route>
          <Route path="/stock-news" element={<StockNewsPage></StockNewsPage>}></Route>
          <Route path="/email-settings" element={<EmailSettingsPage></EmailSettingsPage>}></Route>
          <Route path="/login" element={<LoginPage></LoginPage>}></Route>
          <Route path="/sign-up" element={<SignUpPage></SignUpPage>}></Route>
          <Route
            path="/find-password"
            element={<FindPasswordPage></FindPasswordPage>}
          ></Route>
          <Route path="/find-email" element={<FindEmailPage></FindEmailPage>}></Route>
        </Route>
      </Routes>
    </AuthProvider>
  )
}

export default App
