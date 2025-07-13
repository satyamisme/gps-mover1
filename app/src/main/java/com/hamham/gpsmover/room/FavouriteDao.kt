package com.hamham.gpsmover.room
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

        //insert data to room database
        @Insert(onConflict = OnConflictStrategy.IGNORE)
        suspend fun insertToRoomDatabase(favourite: Favourite) : Long

        // for update single favourite
        @Update
        suspend fun updateUserDetails(favourite: Favourite)

        //delete single favourite
        @Delete
        suspend fun deleteSingleFavourite(favourite: Favourite)

       //get all Favourite inserted to room database...normally this is supposed to be a list of Favourites
        @Transaction
        @Query("SELECT * FROM favourite ORDER BY `order` ASC, id DESC")
        fun getAllFavourites() : Flow<List<Favourite>>

        //get single favourite inserted to room database
        @Transaction
        @Query("SELECT * FROM favourite WHERE id = :id ORDER BY id DESC")
        fun getSingleFavourite(id: Long) : Favourite

        // Update order for a single favourite
        @Query("UPDATE favourite SET `order` = :order WHERE id = :id")
        suspend fun updateOrder(id: Long, order: Int)

        // Update multiple favourites order
        @Update
        suspend fun updateFavourites(favourites: List<Favourite>)

        // Delete all favourites
        @Query("DELETE FROM favourite")
        suspend fun deleteAllFavourites()

        // Bulk insert all favourites
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertAll(favorites: List<Favourite>)

        // Replace all favourites in a single transaction
        @Transaction
        suspend fun replaceAllFavourites(favorites: List<Favourite>) {
            deleteAllFavourites()
            insertAll(favorites)
        }

}