import { useId, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client.js'
import { useCreateItem, useDeleteItem, useRenameItem } from '../api/queries.js'
import { useI18n } from '../i18n/context.js'
import { errorMessage } from '../i18n/errors.js'

const MAX_NAME_LENGTH = 255

// One editable list (categories or ingredients): add, filter, rename and delete.
// `labels` holds the translation keys that differ between the two lists.
export default function NameListSection({ kind, query, labels, linkToRecipes, blockDeleteWhenUsed }) {
  const { t, language } = useI18n()
  const headingId = useId()
  const [filter, setFilter] = useState('')

  const items = query.data ?? []
  const needle = filter.trim().toLocaleLowerCase(language)
  const visible = needle ? items.filter((item) => item.name.toLocaleLowerCase(language).includes(needle)) : items

  return (
    <section className="name-list" aria-labelledby={headingId}>
      <h2 id={headingId}>{t(labels.title)}</h2>

      <AddForm kind={kind} labels={labels} />

      {query.isPending && <p className="hint">…</p>}
      {query.isError && (
        <p className="field-error" role="alert">
          {t('manage.loadFailed', { message: errorMessage(query.error, t) })}
        </p>
      )}

      {query.isSuccess && items.length === 0 && <p className="hint">{t(labels.empty)}</p>}

      {items.length > 0 && (
        <>
          <input
            type="search"
            className="name-filter"
            aria-label={t(labels.filter)}
            placeholder={t(labels.filter)}
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
          />
          {visible.length === 0 ? (
            <p className="hint">{t('manage.noMatches', { filter: filter.trim() })}</p>
          ) : (
            <ul className="name-rows">
              {visible.map((item) => (
                <NameRow
                  key={item.id}
                  kind={kind}
                  item={item}
                  labels={labels}
                  linkToRecipes={linkToRecipes}
                  blockDeleteWhenUsed={blockDeleteWhenUsed}
                />
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}

function validateName(name, t) {
  const trimmed = name.trim()
  if (!trimmed) return t('manage.nameRequired')
  if (trimmed.length > MAX_NAME_LENGTH) return t('validation.tooLong', { max: MAX_NAME_LENGTH })
  return null
}

function saveErrorText(error, t, labels) {
  if (error instanceof ApiError && error.status === 409) return t(labels.taken)
  return t('manage.saveFailed', { message: errorMessage(error, t) })
}

function AddForm({ kind, labels }) {
  const { t } = useI18n()
  const inputId = useId()
  const create = useCreateItem(kind)
  const [name, setName] = useState('')
  const [error, setError] = useState(null)

  async function handleSubmit(event) {
    event.preventDefault()
    const invalid = validateName(name, t)
    if (invalid) {
      setError(invalid)
      return
    }
    setError(null)
    try {
      await create.mutateAsync(name.trim())
      setName('')
    } catch (e) {
      setError(saveErrorText(e, t, labels))
    }
  }

  return (
    <form className="inline-add" onSubmit={handleSubmit} noValidate>
      <label className="visually-hidden" htmlFor={inputId}>
        {t(labels.newItem)}
      </label>
      <input
        id={inputId}
        type="text"
        placeholder={t(labels.newItem)}
        value={name}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${inputId}-error` : undefined}
        onChange={(event) => setName(event.target.value)}
      />
      <button type="submit" className="button" disabled={create.isPending}>
        {t('common.add')}
      </button>
      {error && (
        <p id={`${inputId}-error`} className="field-error inline-add-error">
          {error}
        </p>
      )}
    </form>
  )
}

// A row is in one of four modes: showing, renaming, confirming a delete, or explaining
// why an in-use ingredient can't be deleted
function NameRow({ kind, item, labels, linkToRecipes, blockDeleteWhenUsed }) {
  const { t } = useI18n()
  const rename = useRenameItem(kind)
  const remove = useDeleteItem(kind)
  const [mode, setMode] = useState('view')
  const [draft, setDraft] = useState(item.name)
  const [error, setError] = useState(null)
  const errorId = useId()

  function startRename() {
    setDraft(item.name)
    setError(null)
    setMode('rename')
  }

  // Leaving a mode also drops its error, so a fixed or abandoned rename doesn't leave one behind
  function backToView() {
    setError(null)
    setMode('view')
  }

  function startDelete() {
    setError(null)
    setMode(blockDeleteWhenUsed && item.recipeCount > 0 ? 'blocked' : 'confirm')
  }

  async function saveRename(event) {
    event.preventDefault()
    const invalid = validateName(draft, t)
    if (invalid) {
      setError(invalid)
      return
    }
    if (draft.trim() === item.name) {
      backToView()
      return
    }
    try {
      await rename.mutateAsync({ id: item.id, name: draft.trim() })
      backToView()
    } catch (e) {
      setError(saveErrorText(e, t, labels))
    }
  }

  async function confirmDelete() {
    try {
      await remove.mutateAsync(item.id)
    } catch (e) {
      // Someone added it to a recipe since the list loaded
      if (e instanceof ApiError && e.status === 409) setMode('blocked')
      else setError(t('manage.deleteFailed', { message: errorMessage(e, t) }))
    }
  }

  const usage =
    item.recipeCount === 0 ? (
      <span className="usage">{t('manage.unused')}</span>
    ) : linkToRecipes ? (
      <Link className="usage" to={`/?categoryId=${item.id}`}>
        {t('manage.usedIn', { count: item.recipeCount })}
      </Link>
    ) : (
      <span className="usage">{t('manage.usedIn', { count: item.recipeCount })}</span>
    )

  if (mode === 'rename') {
    return (
      <li className="name-row editing">
        <form className="rename-form" onSubmit={saveRename} noValidate>
          <input
            type="text"
            aria-label={t('manage.renameLabel', { name: item.name })}
            value={draft}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? errorId : undefined}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={(event) => event.key === 'Escape' && backToView()}
            // Focus moves here when Rename is clicked, so typing can start right away
            autoFocus
          />
          <button type="submit" className="button primary" disabled={rename.isPending}>
            {t('common.save')}
          </button>
          <button type="button" className="button ghost" onClick={backToView}>
            {t('common.cancel')}
          </button>
        </form>
        {error && (
          <p id={errorId} className="field-error">
            {error}
          </p>
        )}
      </li>
    )
  }

  return (
    <li className="name-row">
      <div className="name-row-main">
        <span className="name">{item.name}</span>
        {usage}
      </div>
      {mode === 'view' && (
        <div className="row-buttons">
          <button type="button" className="button ghost" onClick={startRename}>
            {t('manage.rename')}
          </button>
          <button
            type="button"
            className="button ghost danger-text"
            aria-label={t('manage.deleteLabel', { name: item.name })}
            onClick={startDelete}
          >
            {t('manage.delete')}
          </button>
        </div>
      )}
      {mode === 'confirm' && (
        <div className="row-confirm" role="group" aria-label={t('manage.confirmDeleteLabel')}>
          <span>
            {item.recipeCount > 0 && labels.deleteConfirmUsed
              ? t(labels.deleteConfirmUsed, { name: item.name, count: item.recipeCount })
              : t('manage.deleteConfirm', { name: item.name })}
          </span>
          <button type="button" className="button danger" onClick={confirmDelete} disabled={remove.isPending}>
            {t('detail.confirmDeleteYes')}
          </button>
          <button type="button" className="button ghost" onClick={backToView}>
            {t('common.cancel')}
          </button>
        </div>
      )}
      {mode === 'blocked' && (
        <div className="row-confirm blocked" role="alert">
          <span>{t('manage.ingredientInUse')}</span>
          <button type="button" className="button ghost" onClick={backToView}>
            {t('common.ok')}
          </button>
        </div>
      )}
      {error && <p className="field-error">{error}</p>}
    </li>
  )
}
