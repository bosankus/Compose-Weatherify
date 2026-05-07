package bose.ankush.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AuthToken entity
 */
@Dao
interface AuthTokenDao {
    /**
     * Insert or replace a token
     * @param token The token to save
     * @return The row ID of the inserted token
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveToken(token: AuthToken): Long

    /**
     * Get the stored token
     * @return The token or null if not found
     */
    @Query("SELECT * FROM auth_tokens WHERE id = 1 LIMIT 1")
    fun getToken(): AuthToken?

    /**
     * Observe if a token exists
     * @return Flow of Boolean indicating if a token exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM auth_tokens WHERE id = 1 LIMIT 1)")
    fun hasToken(): Flow<Boolean>

    /**
     * Delete all tokens
     * @return The number of tokens deleted
     */
    @Query("DELETE FROM auth_tokens")
    fun clearTokens(): Int
}
