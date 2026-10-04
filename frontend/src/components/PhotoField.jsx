import { useId, useState } from 'react'
import { ApiError } from '../api/client.js'
import { useUploadPhoto } from '../api/queries.js'
import { useI18n } from '../i18n/context.js'
import { errorMessage } from '../i18n/errors.js'
import { shrinkPhoto } from '../utils/photo.js'

// The recipe photo: a preview, buttons to upload (or take, on a phone) a photo and to remove it,
// and a field for linking to a photo online instead. `value` is the photo URL.
export default function PhotoField({ value, onChange, error }) {
  const { t } = useI18n()
  const upload = useUploadPhoto()
  const inputId = useId()
  const linkId = useId()
  const [uploadError, setUploadError] = useState(null)
  // An uploaded photo lives under /images; anything else was linked by hand, so show the link field
  const [showLink, setShowLink] = useState(Boolean(value) && !value.startsWith('/images/'))

  async function handleFile(event) {
    const file = event.target.files?.[0]
    // Allow choosing the same file again after an error
    event.target.value = ''
    if (!file) return
    setUploadError(null)
    try {
      onChange(await upload.mutateAsync(await shrinkPhoto(file)))
    } catch (e) {
      if (e instanceof ApiError && e.status === 413) setUploadError(t('form.photoTooLarge'))
      else if (e instanceof ApiError && e.status === 415) setUploadError(t('form.photoType'))
      else setUploadError(t('form.uploadFailed', { message: errorMessage(e, t) }))
    }
  }

  const shownError = uploadError || error
  return (
    <div className="field photo-field">
      <span className="field-label">{t('form.photo')}</span>
      <div className="photo-row">
        {value ? (
          <img className="photo-preview" src={value} alt={t('form.photoPreview')} />
        ) : (
          <div className="photo-preview empty" aria-hidden="true" />
        )}
        <div className="photo-actions">
          {/* One control: the real file input, hidden but focusable, with its label shown as a button.
              On phones the picker also offers the camera. */}
          <input
            id={inputId}
            className="visually-hidden photo-input"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            disabled={upload.isPending}
            onChange={handleFile}
          />
          <label htmlFor={inputId} className="button" aria-disabled={upload.isPending || undefined}>
            {upload.isPending ? t('form.uploading') : value ? t('form.changePhoto') : t('form.uploadPhoto')}
          </label>
          {value && !upload.isPending && (
            <button type="button" className="button ghost danger-text" onClick={() => onChange('')}>
              {t('form.removePhoto')}
            </button>
          )}
          <p className="field-hint">{t('form.photoHint')}</p>
        </div>
      </div>
      {shownError && (
        <p className="field-error" role="alert">
          {shownError}
        </p>
      )}
      {showLink ? (
        <div className="photo-link">
          <label htmlFor={linkId}>{t('form.imageUrl')}</label>
          <input
            id={linkId}
            type="url"
            placeholder="https://…"
            value={value}
            aria-invalid={error ? true : undefined}
            onChange={(event) => onChange(event.target.value)}
          />
        </div>
      ) : (
        <button type="button" className="link-button" onClick={() => setShowLink(true)}>
          {t('form.photoLink')}
        </button>
      )}
    </div>
  )
}
