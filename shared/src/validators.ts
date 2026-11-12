/**
 * Shared Validation Utilities (`shared/src/validators.ts`)
 */

export const USERNAME_REGEX = /^[a-z0-9_]{3,24}$/;

export const RESERVED_USERNAMES = new Set([
  'admin',
  'administrator',
  'support',
  'moderator',
  'mod',
  'adda',
  'official',
  'staff',
  'help',
  'system',
  'root',
  'null',
  'undefined',
]);

export type UsernameValidationError =
  | 'VALID'
  | 'TOO_SHORT'
  | 'TOO_LONG'
  | 'INVALID_CHARS'
  | 'RESERVED';

export function validateUsername(raw: string): UsernameValidationError {
  const trimmed = raw.trim();
  if (trimmed.length < 3) return 'TOO_SHORT';
  if (trimmed.length > 24) return 'TOO_LONG';
  if (!/^[a-z0-9_]+$/.test(trimmed)) return 'INVALID_CHARS';
  if (RESERVED_USERNAMES.has(trimmed)) return 'RESERVED';
  return 'VALID';
}

export function validateDisplayName(raw: string): boolean {
  const trimmed = raw.trim();
  return trimmed.length >= 1 && trimmed.length <= 60;
}
