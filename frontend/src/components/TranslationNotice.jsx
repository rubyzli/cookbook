import { Link } from 'react-router'
import { useMachineTranslate, useTranslationSettings } from '../api/queries.js'
import { useI18n } from '../i18n/context.js'
import { errorMessage } from '../i18n/errors.js'

// Says which language a recipe is shown in, and offers what makes sense next: switching between
// translation and original, translating it, or fixing a translation.
// `recipe` is what is shown; `showingOriginal` is true while the reader asked for the original.
export default function TranslationNotice({ recipe, showingOriginal, onShowOriginal }) {
  const { t, language } = useI18n()
  const settings = useTranslationSettings()
  const machineTranslate = useMachineTranslate(recipe.id)
  const original = t(`language.${recipe.originalLanguage}`)
  const translatePage = `/recipes/${recipe.id}/translate/${language}`

  // Written in the reader's language (or unknown): nothing to say
  if (!recipe.originalLanguage || recipe.originalLanguage === language) return null

  if (showingOriginal) {
    return (
      <div className="translation-notice">
        <span>{t('translation.showingOriginal', { language: original })}</span>
        <button type="button" className="button ghost small" onClick={() => onShowOriginal(false)}>
          {t('translation.showTranslation')}
        </button>
      </div>
    )
  }

  if (recipe.translationStatus) {
    return (
      <div className={recipe.translationOutdated ? 'translation-notice warning' : 'translation-notice'}>
        <span>
          {t(recipe.translationStatus === 'MACHINE' ? 'translation.machine' : 'translation.reviewed', {
            language: original,
          })}
          {recipe.translationOutdated && <> {t('translation.outdated')}</>}
        </span>
        <button type="button" className="button ghost small" onClick={() => onShowOriginal(true)}>
          {t('translation.showOriginal')}
        </button>
        <Link to={translatePage} className="button ghost small">
          {t('translation.edit')}
        </Link>
      </div>
    )
  }

  return (
    <div className="translation-notice">
      <span>{t('translation.onlyOriginal', { language: original })}</span>
      {settings.data?.machineTranslation && (
        <button
          type="button"
          className="button small"
          disabled={machineTranslate.isPending}
          onClick={() => machineTranslate.mutate(language)}
        >
          {machineTranslate.isPending ? t('translation.translating') : t('translation.automatic')}
        </button>
      )}
      <Link to={translatePage} className="button ghost small">
        {t('translation.byHand')}
      </Link>
      {machineTranslate.isError && (
        <p className="field-error" role="alert">
          {t('translation.failed', { message: errorMessage(machineTranslate.error, t) })}
        </p>
      )}
    </div>
  )
}
