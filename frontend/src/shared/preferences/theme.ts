export const THEMES = ["navy", "maroon"] as const;
export type Theme = (typeof THEMES)[number];
export const DEFAULT_THEME: Theme = "navy";
export const THEME_COOKIE = "theme";

export function isTheme(value: unknown): value is Theme {
  return typeof value === "string" && (THEMES as readonly string[]).includes(value);
}
