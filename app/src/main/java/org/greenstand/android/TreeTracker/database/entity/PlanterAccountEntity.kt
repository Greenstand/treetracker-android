package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
@Entity(
    tableName = PlanterAccountEntity.TABLE
)
data class PlanterAccountEntity(
    @PrimaryKey
    @ColumnInfo(name= PLANTER_INFO_ID)
    val planterInfoId: String,
    @ColumnInfo(name = UPLOADED_TREE_COUNT)
    val uploadedTreeCount: Int,
    @ColumnInfo(name = VALIDATED_TREE_COUNT)
    val validatedTreeCount: Int,
    @ColumnInfo(name = TOTAL_AMOUNT_PAID)
    val totalAmountPaid: Double,
    @ColumnInfo(name = PAYMENT_AMOUNT_PENDING)
    val paymentAmountPending: Double,
    @ColumnInfo(name = UPDATED_AT)
    val updatedAt: Long
) {
    companion object {
        const val TABLE = "planter_account"
        // planter_info_id is the planter identifier (referencing email or phone number)
        const val PLANTER_INFO_ID = "planter_info_id"
        const val UPLOADED_TREE_COUNT = "uploaded_tree_count"
        const val VALIDATED_TREE_COUNT = "validated_tree_count"
        const val TOTAL_AMOUNT_PAID = "total_amount_paid"
        const val PAYMENT_AMOUNT_PENDING = "payment_amount_pending"
        const val UPDATED_AT = "updated_at"
    }
}
