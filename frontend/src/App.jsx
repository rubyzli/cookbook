import { Route, Routes } from 'react-router'
import Layout from './components/Layout.jsx'
import EditRecipePage from './pages/EditRecipePage.jsx'
import ManagePage from './pages/ManagePage.jsx'
import NewRecipePage from './pages/NewRecipePage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import RecipeDetailPage from './pages/RecipeDetailPage.jsx'
import RecipeListPage from './pages/RecipeListPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<RecipeListPage />} />
        <Route path="recipes/new" element={<NewRecipePage />} />
        <Route path="recipes/:id" element={<RecipeDetailPage />} />
        <Route path="recipes/:id/edit" element={<EditRecipePage />} />
        <Route path="manage" element={<ManagePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
