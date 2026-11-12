package com.example.services

import com.example.model.AppNotificationItem
import com.example.model.CommentItem
import com.example.model.ContentReport
import com.example.model.FollowRelation
import com.example.model.PostDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// ============================================================================
// FUTURE PHASE SERVICE INTERFACES & STUB STRUCTURES
// As requested in Phase 1 specification:
// PostService, CommentService, FollowService, LikeService, ImageService,
// NotificationService, ReportService, BlockService, VideoService have only
// their interfaces and empty stub structures in Phase 1 so later phases can
// plug in without refactoring the architecture.
// ============================================================================

interface PostService {
  fun observeFeedPosts(limit: Int = 20): Flow<List<PostDraft>>
  suspend fun createPost(content: String, imageUrls: List<String>, categoryId: String?): Result<String>
  suspend fun deletePost(postId: String): Result<Unit>
}

interface CommentService {
  fun observeComments(postId: String): Flow<List<CommentItem>>
  suspend fun addComment(postId: String, text: String): Result<String>
  suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
}

interface FollowService {
  fun observeFollowing(uid: String): Flow<List<FollowRelation>>
  suspend fun followUser(targetUid: String): Result<Unit>
  suspend fun unfollowUser(targetUid: String): Result<Unit>
}

interface LikeService {
  suspend fun likePost(postId: String): Result<Unit>
  suspend fun unlikePost(postId: String): Result<Unit>
  suspend fun hasLikedPost(postId: String, uid: String): Result<Boolean>
}

interface ImageService {
  suspend fun uploadCompressedImage(localUriString: String, folderPath: String): Result<String>
  suspend fun deleteImage(storagePath: String): Result<Unit>
}

interface NotificationService {
  fun observeNotifications(uid: String): Flow<List<AppNotificationItem>>
  suspend fun markNotificationRead(notificationId: String): Result<Unit>
}

interface ReportService {
  suspend fun submitReport(report: ContentReport): Result<String>
}

interface BlockService {
  fun observeBlockedUserIds(uid: String): Flow<Set<String>>
  suspend fun blockUser(targetUid: String): Result<Unit>
  suspend fun unblockUser(targetUid: String): Result<Unit>
}

/**
 * Note: Video features are explicitly excluded from the platform scope
 * ("ভিডিও, অডিও/ভিডিও কল, লাইভ, মনিটাইজেশন, পেমেন্ট নেই"), kept only as an interface
 * per the services/ layer specification.
 */
interface VideoService {
  fun isVideoSupported(): Boolean = false
}

// Empty Phase-1 Stubs returning explicit NotImplementedError ("শীঘ্রই আসছে")
class Phase1PostServiceStub : PostService {
  override fun observeFeedPosts(limit: Int): Flow<List<PostDraft>> = emptyFlow()
  override suspend fun createPost(
    content: String,
    imageUrls: List<String>,
    categoryId: String?
  ): Result<String> = Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 2)"))

  override suspend fun deletePost(postId: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 2)"))
}

class Phase1CommentServiceStub : CommentService {
  override fun observeComments(postId: String): Flow<List<CommentItem>> = emptyFlow()
  override suspend fun addComment(postId: String, text: String): Result<String> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 3)"))

  override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 3)"))
}

class Phase1FollowServiceStub : FollowService {
  override fun observeFollowing(uid: String): Flow<List<FollowRelation>> = emptyFlow()
  override suspend fun followUser(targetUid: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 3)"))

  override suspend fun unfollowUser(targetUid: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 3)"))
}

class Phase1LikeServiceStub : LikeService {
  override suspend fun likePost(postId: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 2)"))

  override suspend fun unlikePost(postId: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 2)"))

  override suspend fun hasLikedPost(postId: String, uid: String): Result<Boolean> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 2)"))
}

class Phase1ImageServiceStub : ImageService {
  override suspend fun uploadCompressedImage(
    localUriString: String,
    folderPath: String
  ): Result<String> = Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 2)"))

  override suspend fun deleteImage(storagePath: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 2)"))
}

class Phase1NotificationServiceStub : NotificationService {
  override fun observeNotifications(uid: String): Flow<List<AppNotificationItem>> = emptyFlow()
  override suspend fun markNotificationRead(notificationId: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 4)"))
}

class Phase1ReportServiceStub : ReportService {
  override suspend fun submitReport(report: ContentReport): Result<String> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 5)"))
}

class Phase1BlockServiceStub : BlockService {
  override fun observeBlockedUserIds(uid: String): Flow<Set<String>> = emptyFlow()
  override suspend fun blockUser(targetUid: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 5)"))

  override suspend fun unblockUser(targetUid: String): Result<Unit> =
    Result.failure(NotImplementedError("শীঘ্রই আসছে (Phase 5)"))
}

class Phase1VideoServiceStub : VideoService
