import { unstable_rethrow } from "next/navigation";

export type LoadResult<T> = { ok: true; data: T } | { ok: false; error: unknown };

/**
 * Runs a data load and returns the result or the error, so pages can fetch first and render after
 * (JSX inside try/catch would not catch render errors anyway).
 * Next.js control flow (redirect to /logout on 401, notFound) is re-thrown, not shown as an error.
 */
export async function load<T>(fetcher: () => Promise<T>): Promise<LoadResult<T>> {
  try {
    return { ok: true, data: await fetcher() };
  } catch (error) {
    unstable_rethrow(error);
    return { ok: false, error };
  }
}
