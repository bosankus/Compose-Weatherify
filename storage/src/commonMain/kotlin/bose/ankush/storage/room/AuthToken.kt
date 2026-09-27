package bose.ankush.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Legacy Room entity retained for schema compatibility (version 3). Token I/O uses EncryptedTokenStorage. */
@Entity(tableName = "auth_tokens")
data class AuthToken(
    @PrimaryKey
    val id: Int = 1,
    val token: String,
    val createdAt: Long = 0L,
)
