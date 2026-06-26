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
    ROUTE("route");
}