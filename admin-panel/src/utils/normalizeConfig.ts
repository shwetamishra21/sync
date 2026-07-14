/**
 * Defensive guard for the theme/layout/branding config props.
 *
 * These configs are typed as plain objects (ThemeConfig / LayoutConfig /
 * BrandingConfig), but that's only a compile-time guarantee. If the API
 * ever returns something else at runtime (e.g. a JSON-encoded string, as
 * happened with the JSONB double-encoding bug), spreading it directly into
 * React state - `{...value, [field]: newValue}` - does NOT throw. For a
 * string, the spread silently expands each character into a numeric-key
 * entry (`{"0": "{", "1": "\"", ...}`), which then gets saved as "valid"
 * but garbage JSON. That's what caused the Layout Editor to silently stop
 * working even after the backend was fixed to stop crashing.
 *
 * Use this to sanitize any config prop before it's used as initial state,
 * so a bad value falls back to `fallback` instead of being spread.
 */
export function asPlainObject<T extends object>(
  value: unknown,
  fallback: T
): T {
  if (
    value !== null &&
    typeof value === "object" &&
    !Array.isArray(value)
  ) {
    return value as T;
  }
  return fallback;
}
