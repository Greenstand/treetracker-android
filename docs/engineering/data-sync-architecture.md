# Data Sync Architecture

Background sync is handled by `TreeSyncWorker` (a `CoroutineWorker`) that runs `SyncDataUseCase`. The sync process executes in this order:

1. Sync Messages: `MessagesRepo.syncMessages()`
2. Upload Device Config: `DeviceConfigUploader.upload()`
3. Upload User Data: `PlanterUploader.upload()` (includes user images)
4. Upload Session Data: `SessionUploader.upload()`
5. Upload Trees: `TreeUploader.uploadLegacyTrees()` and `TreeUploader.uploadTrees()`
6. Upload Location Data: `UploadLocationDataUseCase.execute()`

## Upload Pattern

All uploaders follow this pattern:

1. Fetch data from Room database
2. Upload images to AWS S3 using `UploadImageUseCase` (if applicable)
3. Update local data with image URLs
4. Create JSON bundle with upload data
5. Upload bundle to AWS S3 via `ObjectStorageClient`
6. Update local data with bundle ID and mark as uploaded
7. Delete local image files (if applicable)
