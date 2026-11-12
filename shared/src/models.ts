/**
 * Shared TypeScript Models (`shared/src/models.ts`)
 * Used across `admin-app` (Phase 6) and `functions`.
 */

export type SupportedLanguage = 'bn' | 'en';
export type ThemePreference = 'system' | 'light' | 'dark';

/**
 * Note: No `role` field is stored in `users/{userId}`.
 * Admin/Moderator roles will be enforced via Firebase Auth Custom Claims in Phase 6.
 */
export interface UserDocument {
  uid: string;
  displayName: string;
  username: string;
  email: string;
  photoUrl?: string;
  bio?: string;
  language: SupportedLanguage;
  themeMode: ThemePreference;
  onboardingCompleted: boolean;
  // Read-only system & denormalized counter fields (never written by client)
  followersCount?: number;
  followingCount?: number;
  postsCount?: number;
  isVerified?: boolean;
  isBanned?: boolean;
  createdAt: unknown; // Firestore Timestamp
  updatedAt: unknown; // Firestore Timestamp
}

export interface UsernameDocument {
  uid: string;
  createdAt: unknown; // Firestore Timestamp
}

// Planned entities for Phases 2–6
export interface PostDocument {
  id: string;
  authorUid: string;
  authorUsername: string;
  authorDisplayName: string;
  authorPhotoUrl?: string;
  content: string;
  imageUrls: string[];
  categoryId?: string;
  likesCount: number;
  commentsCount: number;
  bookmarksCount: number;
  isHidden: boolean;
  createdAt: unknown;
  updatedAt: unknown;
}

export interface CommentDocument {
  id: string;
  postId: string;
  authorUid: string;
  authorUsername: string;
  authorDisplayName: string;
  authorPhotoUrl?: string;
  text: string;
  createdAt: unknown;
}

export interface LikeDocument {
  postId: string;
  uid: string;
  createdAt: unknown;
}

export interface FollowDocument {
  followerUid: string;
  followingUid: string;
  createdAt: unknown;
}

export interface BookmarkDocument {
  uid: string;
  postId: string;
  createdAt: unknown;
}

export interface NotificationDocument {
  id: string;
  recipientUid: string;
  actorUid: string;
  actorUsername: string;
  actorDisplayName: string;
  type: 'like' | 'comment' | 'follow' | 'system';
  targetId?: string;
  isRead: boolean;
  createdAt: unknown;
}

export interface ReportDocument {
  id: string;
  reporterUid: string;
  targetType: 'post' | 'comment' | 'user';
  targetId: string;
  reason: string;
  details?: string;
  status: 'pending' | 'reviewed' | 'dismissed' | 'actioned';
  createdAt: unknown;
}

export interface BlockDocument {
  blockerUid: string;
  blockedUid: string;
  createdAt: unknown;
}

export interface CategoryDocument {
  id: string;
  slug: string;
  nameBn: string;
  nameEn: string;
  postsCount: number;
  isActive: boolean;
}

export interface AuditLogDocument {
  id: string;
  adminUid: string;
  action: string;
  targetType: string;
  targetId: string;
  reason: string;
  createdAt: unknown;
}

export interface AppSettingsDocument {
  maintenanceMode: boolean;
  allowNewRegistrations: boolean;
  minSupportedVersion: string;
  updatedAt: unknown;
}
