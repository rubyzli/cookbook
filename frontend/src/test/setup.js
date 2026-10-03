import * as matchers from '@testing-library/jest-dom/matchers'
import { cleanup } from '@testing-library/react'
import { afterEach, expect, vi } from 'vitest'

expect.extend(matchers)

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  // The chosen language is remembered in localStorage; start each test from the browser default
  localStorage.clear()
})
