/** Mirrors the backend's RFC 9457 ProblemDetail error body. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
}

export class ApiError extends Error {
  constructor(public readonly problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with ${problem.status}`);
    this.name = "ApiError";
  }
}

type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export interface ApiClientOptions {
  baseUrl: string;
  /** Extra headers per request (the server adds the token here). */
  headers?: () => Promise<Record<string, string>>;
  /** Called on 401 before the error is thrown (send the user to the login page). */
  onUnauthorized?: () => never | void;
}

/** One request/error implementation shared by the browser and the server clients. */
export function createApiClient({ baseUrl, headers, onUnauthorized }: ApiClientOptions) {
  async function request<T>(method: Method, path: string, body?: unknown, init?: RequestInit): Promise<T> {
    const response = await fetch(`${baseUrl}${path}`, {
      ...init,
      method,
      headers: {
        Accept: "application/json",
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(headers ? await headers() : {}),
        ...init?.headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    if (response.status === 401 && onUnauthorized) {
      onUnauthorized();
    }
    if (!response.ok) {
      const problem: ProblemDetail = await response
        .json()
        .catch(() => ({ status: response.status, title: response.statusText }));
      throw new ApiError({ ...problem, status: response.status });
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  return {
    get: <T>(path: string, init?: RequestInit) => request<T>("GET", path, undefined, init),
    post: <T>(path: string, body?: unknown, init?: RequestInit) => request<T>("POST", path, body, init),
    put: <T>(path: string, body?: unknown, init?: RequestInit) => request<T>("PUT", path, body, init),
    patch: <T>(path: string, body?: unknown, init?: RequestInit) => request<T>("PATCH", path, body, init),
    delete: <T>(path: string, init?: RequestInit) => request<T>("DELETE", path, undefined, init),
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;
