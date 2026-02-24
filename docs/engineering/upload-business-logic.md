# Upload Business Logic

The data upload process is managed by `TreeSyncWorker`, a `CoroutineWorker` that runs in the background. The worker triggers `SyncDataUseCase`, which orchestrates the entire upload process.

The upload process is as follows:

1. Sync Messages: `MessagesRepo.syncMessages()`
2. Upload Device Config: `DeviceConfigUploader.upload()`
3. Upload User Data: `PlanterUploader.upload()` (includes user images)
4. Upload Session Data: `SessionUploader.upload()`
5. Upload Trees: `TreeUploader.uploadLegacyTrees()` and `TreeUploader.uploadTrees()`
6. Upload Location Data: `UploadLocationDataUseCase.execute()`

## General Upload Pattern

The uploaders (`TreeUploader`, `PlanterUploader`, `SessionUploader`) follow a similar pattern:

1. Fetch data from the local Room database
2. Upload images to AWS S3 using `UploadImageUseCase` and get image URLs (if applicable)
3. Update local data with image URLs
4. Create a JSON bundle containing upload data
5. Upload the JSON bundle to AWS S3 using `ObjectStorageClient`
6. Update local data with bundle ID and mark it as uploaded
7. Delete uploaded local image files (if applicable)
