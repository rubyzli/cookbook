import { Link, Outlet } from 'react-router'

export default function Layout() {
  return (
    <>
      <header className="site-header">
        <div className="container">
          <Link to="/" className="site-title">
            Family Cookbook
          </Link>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  )
}
