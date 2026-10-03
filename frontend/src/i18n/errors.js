// Text for an error from the API client. Errors the client detects itself are translated;
// otherwise the server's detail is shown as sent.
export function errorMessage(error, t) {
  if (error?.kind === 'network') return t('errors.network')
  if (error?.kind === 'gateway') return t('errors.gateway')
  if (error?.detail) return error.detail
  if (error?.status) return t('errors.status', { status: error.status })
  return error?.message ?? ''
}
