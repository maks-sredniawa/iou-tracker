package app.ioutracker.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    /** The Currency's ISO 4217 code, or null when the phone's region named none. */
    @Query("SELECT code FROM currency WHERE id = $CURRENCY_ROW_ID")
    fun observeCode(): Flow<String?>
}
