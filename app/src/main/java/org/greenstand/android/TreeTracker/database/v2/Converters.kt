package org.greenstand.android.TreeTracker.database.v2

import androidx.room.TypeConverter
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterCheckInId
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterInfoId
import org.greenstand.android.TreeTracker.database.v2.entity.TreeCaptureId

class Converters {
    @TypeConverter
    fun fromPlanterCheckInId(value: PlanterCheckInId?): Long? {
        return value?.value
    }

    @TypeConverter
    fun toPlanterCheckInId(value: Long?): PlanterCheckInId? {
        return value?.let { PlanterCheckInId(it) }
    }

    @TypeConverter
    fun fromPlanterInfoId(value: PlanterInfoId?): Long? {
        return value?.value
    }

    @TypeConverter
    fun toPlanterInfoId(value: Long?): PlanterInfoId? {
        return value?.let { PlanterInfoId(it) }
    }

    @TypeConverter
    fun fromTreeCaptureId(value: TreeCaptureId?): Long? {
        return value?.value
    }

    @TypeConverter
    fun toTreeCaptureId(value: Long?): TreeCaptureId? {
        return value?.let { TreeCaptureId(it) }
    }
}