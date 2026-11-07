export type LoadResult<T> = { ok: true; data: T } | { ok: false; error: unknown };

/**
 * Runs a data load and returns the result or the error, so pages can fetch first and render after
 * (JSX inside try/catch would not catch render errors anyway).
 */
export async function load<T>(fetcher: () => Promise<T>): Promise<LoadResult<T>> {
  try {
    return { ok: true, data: await fetcher() };
  } catch (error) {
    return { ok: false, error };
  }
}
