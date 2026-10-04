import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useDeleteRecipe, useRecipe } from '../api/queries.js'
import CategoryChips from '../components/CategoryChips.jsx'
import RecipeImage from '../components/RecipeImage.jsx'
import RecipeMeta from '../components/RecipeMeta.jsx'
import StatusMessage from '../components/StatusMessage.jsx'
import TranslationNotice from '../components/TranslationNotice.jsx'
import { useI18n } from '../i18n/context.js'
import { errorMessage } from '../i18n/errors.js'
import { formatAmount } from '../utils/format.js'
import { translateUnit } from '../utils/units.js'

export default function RecipeDetailPage() {
  const { t } = useI18n()
  const { id } = useParams()
  // The reader can switch a translated recipe back to its original language
  const [showingOriginal, setShowingOriginal] = useState(false)
  const recipe = useRecipe(id, { original: showingOriginal })

  return (
    <>
      <Link to="/" className="back-link">
        {t('common.backToRecipes')}
      </Link>
      <RecipeDetailContent query={recipe} showingOriginal={showingOriginal} onShowOriginal={setShowingOriginal} />
    </>
  )
}

function RecipeDetailContent({ query, showingOriginal, onShowOriginal }) {
  const { t } = useI18n()
  if (query.isPending) {
    return <StatusMessage>{t('common.loadingRecipe')}</StatusMessage>
  }
  if (query.isError) {
    // A malformed id gets a 400 from the backend; treat it the same as a missing recipe
    const missing = query.error.status === 404 || query.error.status === 400
    return (
      <StatusMessage role="alert">
        {missing ? (
          <p>{t('common.recipeMissing')}</p>
        ) : (
          <>
            <p>{t('detail.loadFailed')}</p>
            <p className="hint">{errorMessage(query.error, t)}</p>
          </>
        )}
      </StatusMessage>
    )
  }

  const recipe = query.data
  return (
    <article className="recipe-detail">
      <header className={recipe.imageUrl ? 'recipe-detail-header with-image' : 'recipe-detail-header'}>
        {/* No placeholder tile here: without a photo the title simply comes first */}
        {recipe.imageUrl && (
          <div className="recipe-hero">
            <RecipeImage recipe={recipe} className="recipe-detail-image" />
          </div>
        )}
        <div className="recipe-detail-heading">
          <CategoryChips categories={recipe.categories} />
          <h1 lang={recipe.language}>{recipe.name}</h1>
          {recipe.description && <p className="description">{recipe.description}</p>}
          <RecipeMeta recipe={recipe} showTotal pills />
          <RecipeActions recipe={recipe} />
        </div>
      </header>

      <TranslationNotice recipe={recipe} showingOriginal={showingOriginal} onShowOriginal={onShowOriginal} />

      <div className="recipe-detail-body" lang={recipe.language}>
        <section aria-labelledby="ingredients-heading">
          <h2 id="ingredients-heading">{t('detail.ingredients')}</h2>
          {recipe.ingredients.length === 0 ? (
            <p className="hint">{t('detail.noIngredients')}</p>
          ) : (
            groupConsecutive(recipe.ingredients).map((section, sectionIndex) => (
              // Index as key: the same group name can come back later in the list
              <div key={sectionIndex} className="ingredient-group-block">
                {section.group && <h3 className="ingredient-group-heading">{section.group}</h3>}
                <ul className="ingredient-list">
                  {section.lines.map((line, index) => (
                    // Index as key: the same ingredient can appear on several lines
                    <IngredientLine key={index} line={line} from={recipe.originalLanguage} to={recipe.language} />
                  ))}
                </ul>
              </div>
            ))
          )}
        </section>

        <div className="recipe-detail-column">
          <section aria-labelledby="instructions-heading">
            <h2 id="instructions-heading">{t('detail.instructions')}</h2>
            <Instructions text={recipe.instructions} />
          </section>
          <Notes text={recipe.notes} />
        </div>
      </div>
    </article>
  )
}

// Delete asks for confirmation in place, since browser confirm() dialogs are easy to click through
function RecipeActions({ recipe }) {
  const { t } = useI18n()
  const [confirming, setConfirming] = useState(false)
  const deleteRecipe = useDeleteRecipe(recipe.id)
  const navigate = useNavigate()

  async function confirmDelete() {
    try {
      await deleteRecipe.mutateAsync()
      navigate('/', { replace: true })
    } catch {
      // The error is shown below from deleteRecipe.error
    }
  }

  if (confirming) {
    return (
      <div className="recipe-actions confirm" role="group" aria-label={t('detail.confirmDeleteLabel')}>
        <span>{t('detail.confirmDelete', { name: recipe.name })}</span>
        <button type="button" className="button danger" onClick={confirmDelete} disabled={deleteRecipe.isPending}>
          {deleteRecipe.isPending ? t('detail.deleting') : t('detail.confirmDeleteYes')}
        </button>
        <button type="button" className="button ghost" onClick={() => setConfirming(false)}>
          {t('common.cancel')}
        </button>
        {deleteRecipe.isError && (
          <p className="field-error" role="alert">
            {t('detail.deleteFailed', { message: errorMessage(deleteRecipe.error, t) })}
          </p>
        )}
      </div>
    )
  }
  return (
    <div className="recipe-actions">
      <Link to={`/recipes/${recipe.id}/edit`} className="button">
        {t('detail.edit')}
      </Link>
      <button type="button" className="button ghost danger-text" onClick={() => setConfirming(true)}>
        {t('detail.delete')}
      </button>
    </div>
  )
}

// Units follow fixed rules when the recipe is shown in another language than it was written in
function IngredientLine({ line, from, to }) {
  const { language } = useI18n()
  const { amount, unit } = translateUnit(line.amount, line.unit, from, to)
  return (
    <li>
      {/* Ticking off ingredients while cooking; kept only until the page is left */}
      <label className="ingredient-check">
        <input type="checkbox" />
        <span>
          <span className="amount">{formatAmount(amount, unit, language)}</span> {line.name}
        </span>
      </label>
    </li>
  )
}

// Consecutive lines with the same group become one section: [{ group, lines }]
function groupConsecutive(lines) {
  const sections = []
  for (const line of lines) {
    const group = line.group ?? null
    const last = sections.at(-1)
    if (last && last.group === group) last.lines.push(line)
    else sections.push({ group, lines: [line] })
  }
  return sections
}

// Each non-empty line is its own paragraph; nothing is shown without notes
function Notes({ text }) {
  const { t } = useI18n()
  const paragraphs = (text ?? '').split('\n').map((line) => line.trim()).filter(Boolean)
  if (paragraphs.length === 0) return null
  return (
    <section aria-labelledby="notes-heading" className="recipe-notes">
      <h2 id="notes-heading">{t('detail.notes')}</h2>
      {paragraphs.map((paragraph, index) => (
        <p key={index}>{paragraph}</p>
      ))}
    </section>
  )
}

// Each non-empty line is a step; a single line stays a paragraph
function Instructions({ text }) {
  const { t } = useI18n()
  const steps = (text ?? '').split('\n').map((line) => line.trim()).filter(Boolean)
  if (steps.length === 0) {
    return <p className="hint">{t('detail.noInstructions')}</p>
  }
  if (steps.length === 1) {
    return <p className="instructions">{steps[0]}</p>
  }
  return (
    <ol className="steps">
      {steps.map((step, index) => (
        <li key={index}>{step}</li>
      ))}
    </ol>
  )
}
