import { Link } from 'react-router'
import StatusMessage from '../components/StatusMessage.jsx'
import { useI18n } from '../i18n/context.js'

export default function NotFoundPage() {
  const { t } = useI18n()
  return (
    <StatusMessage>
      <p>{t('notFound.title')}</p>
      <Link to="/">{t('notFound.back')}</Link>
    </StatusMessage>
  )
}
