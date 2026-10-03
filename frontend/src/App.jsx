import { Route, Routes } from 'react-router'
import Layout from './components/Layout.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import RecipeDetailPage from './pages/RecipeDetailPage.jsx'
import RecipeListPage from './pages/RecipeListPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<RecipeListPage />} />
        <Route path="recipes/:id" element={<RecipeDetailPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
