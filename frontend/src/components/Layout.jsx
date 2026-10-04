import { Link, NavLink, Outlet } from 'react-router'
import { useI18n } from '../i18n/context.js'
import Icon from './Icon.jsx'
import { LANGUAGES } from '../i18n/translate.js'

export default function Layout() {
  const { language, setLanguage, t } = useI18n()
  return (
    <>
      <header className="site-header">
        <div className="container header-row">
          <Link to="/" className="site-title">
            <Icon name="logo" className="site-logo" />
            {t('app.title')}
          </Link>
          <nav className="site-nav" aria-label={t('app.title')}>
            <NavLink to="/" end>
              {t('nav.recipes')}
            </NavLink>
            <NavLink to="/manage">{t('nav.manage')}</NavLink>
          </nav>
          <label className="visually-hidden" htmlFor="language">
            {t('nav.language')}
          </label>
          <select
            id="language"
            className="language-select"
            value={language}
            onChange={(event) => setLanguage(event.target.value)}
          >
            {LANGUAGES.map(({ code, name }) => (
              <option key={code} value={code} lang={code}>
                {name}
              </option>
            ))}
          </select>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  )
}
