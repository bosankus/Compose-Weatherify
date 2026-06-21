package bose.ankush.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_tokens")
data class AuthToken(
    @PrimaryKey
    val id: Int = 1, // We only need one token, so use a fixed ID
    val token: String,
    val createdAt: Long = System.currentTimeMillis(),
)
