import { Route, Routes } from 'react-router-dom'
import EmailSettingsPage from './pages/email-settings/EmailSettingsPage'
import HomePage from './pages/home/HomePage'
import SignUpPage from './pages/sign-up/SignUpPage'
import StockNewsPage from './pages/stock-news/StockNewsPage'
import StockSearchPage from './pages/stock-search/StockSearchPage'
import WatchlistPage from './pages/watchlist/WatchlistPage'
import MainLayout from './layouts/MainLayout'
import LoginPage from './pages/login/LoginPage'
import ForgotPasswordPage from './pages/forgot-password/ForgotPasswordPage'
import FindEmailPage from './pages/find-email/FindEmailPage'

function App() {
  return (
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
          path="/forgot-password"
          element={<ForgotPasswordPage></ForgotPasswordPage>}
        ></Route>
        <Route path="/find-email" element={<FindEmailPage></FindEmailPage>}></Route>
      </Route>
    </Routes>
  )
}

export default App
