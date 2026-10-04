import { useId, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import {
  useDeleteTranslation,
  useMachineTranslate,
  useRecipeTranslations,
  useSaveTranslation,
  useTranslationSettings,
} from '../api/queries.js'
import StatusMessage from '../components/StatusMessage.jsx'
import { useI18n } from '../i18n/context.js'
import { errorMessage } from '../i18n/errors.js'
import { LANGUAGES } from '../i18n/translate.js'

// Translating one recipe into one language: the original next to editable fields, which can be
// filled in automatically and then checked
export default function TranslatePage() {
  const { t } = useI18n()
  const { id, language: target } = useParams()
  const data = useRecipeTranslations(id)

  if (data.isPending) return <StatusMessage>{t('common.loadingRecipe')}</StatusMessage>
  if (data.isError) {
    return (
      <StatusMessage role="alert">
        {data.error.status === 404 || data.error.status === 400
          ? t('common.recipeMissing')
          : t('form.loadFailed', { message: errorMessage(data.error, t) })}
      </StatusMessage>
    )
  }

  const { originalLanguage, original, translations } = data.data
  const existing = translations.find((translation) => translation.language === target)
  return (
    <>
      <Link to={`/recipes/${id}`} className="back-link">
        {t('form.backToRecipe')}
      </Link>
      <h1 className="form-title">{t('translate.title', { name: original.name })}</h1>
      <TargetLanguages recipeId={id} originalLanguage={originalLanguage} target={target} />
      {target === originalLanguage ? (
        <p className="hint">{t('translate.sameLanguage', { language: t(`language.${originalLanguage}`) })}</p>
      ) : (
        // Keyed so the fields start over when another language or a new version comes in
        <TranslationEditor
          key={`${target}-${existing?.updatedAt ?? 'new'}`}
          recipeId={id}
          target={target}
          originalLanguage={originalLanguage}
          original={original}
          existing={existing}
        />
      )}
    </>
  )
}

function TargetLanguages({ recipeId, originalLanguage, target }) {
  const { t } = useI18n()
  return (
    <nav className="target-languages" aria-label={t('translate.into')}>
      <span className="hint">{t('translate.into')}</span>
      {LANGUAGES.filter(({ code }) => code !== originalLanguage).map(({ code }) => (
        <Link
          key={code}
          to={`/recipes/${recipeId}/translate/${code}`}
          className="filter-chip"
          aria-current={code === target ? 'page' : undefined}
        >
          {t(`language.${code}`)}
        </Link>
      ))}
    </nav>
  )
}

function fromTranslation(original, translation) {
  return {
    name: translation?.name ?? '',
    description: translation?.description ?? '',
    instructions: translation?.instructions ?? '',
    notes: translation?.notes ?? '',
    groups: Object.fromEntries(original.groups.map((group) => [group, translation?.groups?.[group] ?? ''])),
  }
}

function TranslationEditor({ recipeId, target, originalLanguage, original, existing }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const settings = useTranslationSettings()
  const machine = useMachineTranslate(recipeId)
  const save = useSaveTranslation(recipeId)
  const remove = useDeleteTranslation(recipeId)
  const [values, setValues] = useState(() => fromTranslation(original, existing))
  const [confirming, setConfirming] = useState(null) // 'machine' | 'delete' | null
  const [nameError, setNameError] = useState(null)
  const [error, setError] = useState(null)

  const set = (field, value) => setValues((current) => ({ ...current, [field]: value }))
  const originalName = t(`language.${originalLanguage}`)
  const targetName = t(`language.${target}`)

  async function fillAutomatically() {
    setConfirming(null)
    setError(null)
    try {
      setValues(fromTranslation(original, await machine.mutateAsync(target)))
    } catch (e) {
      setError(t('translation.failed', { message: errorMessage(e, t) }))
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!values.name.trim()) {
      setNameError(t('manage.nameRequired'))
      return
    }
    setNameError(null)
    setError(null)
    try {
      await save.mutateAsync({ language: target, translation: { ...values, status: 'REVIEWED' } })
      navigate(`/recipes/${recipeId}`)
    } catch (e) {
      setError(t('manage.saveFailed', { message: errorMessage(e, t) }))
    }
  }

  async function deleteTranslation() {
    try {
      await remove.mutateAsync(target)
      navigate(`/recipes/${recipeId}`)
    } catch (e) {
      setError(t('manage.deleteFailed', { message: errorMessage(e, t) }))
    }
  }

  const status = !existing
    ? t('translate.statusNone')
    : existing.status === 'MACHINE'
      ? t('translate.statusMachine')
      : t('translate.statusReviewed')

  return (
    <form className="recipe-form translate-form" onSubmit={handleSubmit} noValidate>
      <p className={existing?.outdated ? 'translation-notice warning' : 'translation-notice'} role="status">
        <span>
          {status}
          {existing?.outdated && <> {t('translate.statusOutdated')}</>}
        </span>
      </p>

      {settings.data?.machineTranslation ? (
        confirming === 'machine' ? (
          <div className="row-confirm" role="group" aria-label={t('translate.machine')}>
            <span>{t('translate.machineReplaces')}</span>
            <button type="button" className="button danger" onClick={fillAutomatically}>
              {t('translate.machineConfirm')}
            </button>
            <button type="button" className="button ghost" onClick={() => setConfirming(null)}>
              {t('common.cancel')}
            </button>
          </div>
        ) : (
          <div>
            <button
              type="button"
              className="button"
              disabled={machine.isPending}
              onClick={() => (existing ? setConfirming('machine') : fillAutomatically())}
            >
              {machine.isPending ? t('translation.translating') : t('translate.machine')}
            </button>
          </div>
        )
      ) : (
        settings.isSuccess && <p className="hint">{t('translate.noMachine')}</p>
      )}

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <div className="translate-columns" aria-hidden="true">
        <span>{t('translate.original', { language: originalName })}</span>
        <span>{t('translate.translation', { language: targetName })}</span>
      </div>

      <TranslateField label={t('form.name')} original={original.name} error={nameError}>
        {(props) => <input {...props} type="text" value={values.name} onChange={(e) => set('name', e.target.value)} />}
      </TranslateField>
      {(original.description || values.description) && (
        <TranslateField label={t('form.description')} original={original.description}>
          {(props) => (
            <input {...props} type="text" value={values.description} onChange={(e) => set('description', e.target.value)} />
          )}
        </TranslateField>
      )}
      {original.groups.length > 0 && (
        <fieldset className="translate-groups">
          <legend>{t('translate.groups')}</legend>
          {original.groups.map((group) => (
            <TranslateField key={group} label={group} original={group} hideLabel>
              {(props) => (
                <input
                  {...props}
                  type="text"
                  value={values.groups[group]}
                  onChange={(e) => set('groups', { ...values.groups, [group]: e.target.value })}
                />
              )}
            </TranslateField>
          ))}
        </fieldset>
      )}
      <TranslateField label={t('form.steps')} original={original.instructions}>
        {(props) => (
          <textarea
            {...props}
            rows={estimatedRows(original.instructions, 6)}
            value={values.instructions}
            onChange={(e) => set('instructions', e.target.value)}
          />
        )}
      </TranslateField>
      {(original.notes || values.notes) && (
        <TranslateField label={t('form.notes')} original={original.notes}>
          {(props) => (
            <textarea {...props} rows={estimatedRows(original.notes, 3)} value={values.notes} onChange={(e) => set('notes', e.target.value)} />
          )}
        </TranslateField>
      )}

      <div className="form-actions">
        <button type="submit" className="button primary" disabled={save.isPending}>
          {save.isPending ? t('form.saving') : t('translate.save')}
        </button>
        <Link to={`/recipes/${recipeId}`} className="button ghost">
          {t('common.cancel')}
        </Link>
        {existing &&
          (confirming === 'delete' ? (
            <span className="row-confirm inline" role="group" aria-label={t('detail.confirmDeleteLabel')}>
              <button type="button" className="button danger" onClick={deleteTranslation}>
                {t('detail.confirmDeleteYes')}
              </button>
              <button type="button" className="button ghost" onClick={() => setConfirming(null)}>
                {t('common.cancel')}
              </button>
            </span>
          ) : (
            <button type="button" className="button ghost danger-text push-right" onClick={() => setConfirming('delete')}>
              {t('translate.delete')}
            </button>
          ))}
      </div>
    </form>
  )
}

// Roughly as many rows as the original takes up, counting wrapped lines, so the two sides line up
// (browsers that support field-sizing grow the box with its content anyway)
function estimatedRows(text, minimum) {
  const wrapped = (text ?? '').split('\n').reduce((rows, line) => rows + Math.max(1, Math.ceil(line.length / 55)), 0)
  return Math.max(minimum, wrapped + 1)
}

// One row: the original text (read-only) next to its translation
function TranslateField({ label, original, error, hideLabel = false, children }) {
  const id = useId()
  return (
    <div className="translate-field">
      <label htmlFor={id} className={hideLabel ? 'visually-hidden' : undefined}>
        {label}
      </label>
      <div className="translate-original">{original}</div>
      <div className="translate-input">
        {children({
          id,
          'aria-invalid': error ? true : undefined,
          'aria-describedby': error ? `${id}-error` : undefined,
        })}
        {error && (
          <p id={`${id}-error`} className="field-error">
            {error}
          </p>
        )}
      </div>
    </div>
  )
}
