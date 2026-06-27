package org.greenstand.android.TreeTracker.analytics

enum class CrashKey(val toString: String) {
    IS_SYNCING("is_syncing"),
    USER_WALLET("user_wallet"),
    POWER_USER_WALLET("power_user_wallet"),
    DESTINATION_WALLET("destination_wallet"),
    SESSION_NOTE("session_note"),
    ORG_NAME("org_name"),
    IS_IN_SESSION("is_in_session"),
    LAST_ROUTE("last_route"),
    ROUTE("route"),

    CAPTURE_SESSION_UUID("capture_session_uuid"),
    PENDING_UPLOAD_COUNT("pending_upload_count"),
    LAST_SYNC_TIMESTAMP("last_sync_timestamp"),
    INSTALLATION_ID("installation_id"),
    IS_ONLINE("is_online"),
    APP_VERSION("app_version"),
    BUILD_TYPE("build_type"),
    UPLOAD_QUEUE_SNAPSHOT("upload_queue_snapshot"),
}