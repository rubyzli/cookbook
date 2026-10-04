import { afterEach, describe, expect, it, vi } from 'vitest'
import { shrinkPhoto } from './photo.js'

const photo = (size, type = 'image/jpeg', name = 'pie.jpg') => new File([new Uint8Array(size)], name, { type })

// A canvas that records its size and "encodes" to a blob of the given size
function stubCanvas(blobSize) {
  const canvas = {
    width: 0,
    height: 0,
    getContext: () => ({ drawImage: vi.fn() }),
    toBlob: (callback) => callback(new Blob([new Uint8Array(blobSize)], { type: 'image/jpeg' })),
  }
  const original = document.createElement.bind(document)
  vi.spyOn(document, 'createElement').mockImplementation((tag) => (tag === 'canvas' ? canvas : original(tag)))
  return canvas
}

describe('shrinkPhoto', () => {
  afterEach(() => vi.restoreAllMocks())

  it('scales a big photo down to 1600 px on its longest side, as JPEG', async () => {
    vi.stubGlobal('createImageBitmap', vi.fn(async () => ({ width: 4000, height: 3000, close: vi.fn() })))
    const canvas = stubCanvas(300_000)

    const result = await shrinkPhoto(photo(4_000_000, 'image/png', 'IMG_1234.png'))

    expect([canvas.width, canvas.height]).toEqual([1600, 1200])
    expect(result.type).toBe('image/jpeg')
    expect(result.name).toBe('IMG_1234.jpg')
    expect(result.size).toBe(300_000)
  })

  it('keeps small photos as they are', async () => {
    vi.stubGlobal('createImageBitmap', vi.fn(async () => ({ width: 800, height: 600, close: vi.fn() })))
    const original = photo(200_000)

    expect(await shrinkPhoto(original)).toBe(original)
  })

  it('keeps the original when shrinking would not make it smaller', async () => {
    vi.stubGlobal('createImageBitmap', vi.fn(async () => ({ width: 3000, height: 2000, close: vi.fn() })))
    stubCanvas(900_000)
    const original = photo(800_000)

    expect(await shrinkPhoto(original)).toBe(original)
  })

  it('leaves GIFs alone, since they may be animated', async () => {
    const decode = vi.fn()
    vi.stubGlobal('createImageBitmap', decode)
    const gif = photo(3_000_000, 'image/gif', 'dance.gif')

    expect(await shrinkPhoto(gif)).toBe(gif)
    expect(decode).not.toHaveBeenCalled()
  })

  it('falls back to the original where the browser cannot decode it', async () => {
    vi.stubGlobal('createImageBitmap', vi.fn(async () => Promise.reject(new Error('unsupported'))))
    const heic = photo(2_000_000, 'image/heic', 'IMG.heic')

    expect(await shrinkPhoto(heic)).toBe(heic)
  })

  it('falls back to the original in browsers without createImageBitmap', async () => {
    vi.stubGlobal('createImageBitmap', undefined)
    const original = photo(5_000_000)

    expect(await shrinkPhoto(original)).toBe(original)
  })
})
