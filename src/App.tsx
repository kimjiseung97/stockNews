import { Route, Routes } from 'react-router-dom'
import EmailSettingsPage from './pages/email-settings/EmailSettingsPage'
import HomePage from './pages/home/HomePage'
import SignUpPage from './pages/sign-up/SignUpPage'
import StockNewsPage from './pages/stock-news/StockNewsPage'
import StockSearchPage from './pages/stock-search/StockSearchPage'
import WatchlistPage from './pages/watchlist/WatchlistPage'
import MainLayout from './layouts/MainLayout'
import LoginPage from './pages/login/LoginPage'
import FindPasswordPage from './pages/find-password/FindPasswordPage'
import FindEmailPage from './pages/find-email/FindEmailPage'
import { AuthProvider } from './contexts/AuthContext'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<MainLayout></MainLayout>}>
        <Route path="/" element={<HomePage></HomePage>}></Route>
        <Route path="/stock-search" element={<StockSearchPage></StockSearchPage>}></Route>
        <Route path="/watchlist" element={<WatchlistPage></WatchlistPage>}></Route>
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
