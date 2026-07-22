import { Route, Routes } from "react-router-dom";
import EmailSettingsPage from "./pages/email-settings/EmailSettingsPage";
import HomePage from "./pages/home/HomePage";
import SignUpPage from "./pages/sign-up/SignUpPage";
import StockNewsPage from "./pages/stock-news/StockNewsPage";
import StockSearchPage from "./pages/stock-search/StockSearchPage";
import WatchlistPage from "./pages/watchlist/WatchlistPage";
import MainLayout from "./layouts/MainLayout";

function App() {
  return (
    <Routes>
      <Route element={<MainLayout></MainLayout>}>
        <Route path="/" element={<HomePage></HomePage>}></Route>
        <Route
          path="/stock-search"
          element={<StockSearchPage></StockSearchPage>}
        ></Route>
        <Route
          path="/watchlist"
          element={<WatchlistPage></WatchlistPage>}
        ></Route>
        <Route
          path="/stock-news"
          element={<StockNewsPage></StockNewsPage>}
        ></Route>
        <Route
          path="/email-settings"
          element={<EmailSettingsPage></EmailSettingsPage>}
        ></Route>
        <Route path="/sign-up" element={<SignUpPage></SignUpPage>}></Route>
      </Route>
    </Routes>
  );
}

export default App;
