import { Link } from 'react-router'
import StatusMessage from '../components/StatusMessage.jsx'

export default function NotFoundPage() {
  return (
    <StatusMessage>
      <p>Page not found.</p>
      <Link to="/">Back to all recipes</Link>
    </StatusMessage>
  )
}
