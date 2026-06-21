package bose.ankush.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthTokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveToken(token: AuthToken): Long

    @Query("SELECT * FROM auth_tokens WHERE id = 1 LIMIT 1")
    fun getToken(): AuthToken?

    @Query("SELECT EXISTS(SELECT 1 FROM auth_tokens WHERE id = 1 LIMIT 1)")
    fun hasToken(): Flow<Boolean>

    @Query("DELETE FROM auth_tokens")
    fun clearTokens(): Int
}
