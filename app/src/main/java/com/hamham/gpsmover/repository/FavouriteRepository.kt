package com.hamham.gpsmover.repository


import androidx.annotation.WorkerThread
import com.hamham.gpsmover.room.Favourite
import com.hamham.gpsmover.room.FavouriteDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FavouriteRepository @Inject constructor(private val favouriteDao: FavouriteDao) {

    val getAllFavourites: Flow<List<Favourite>> get() =  favouriteDao.getAllFavourites()

        @Suppress("RedundantSuspendModifier")
        @WorkerThread
        suspend fun addNewFavourite(favourite: Favourite) : Long {
            return favouriteDao.insertToRoomDatabase(favourite)
        }

        suspend fun deleteFavourite(favourite: Favourite) {
          favouriteDao.deleteSingleFavourite(favourite)
       }

       fun getSingleFavourite(id: Long) : Favourite {
       return favouriteDao.getSingleFavourite(id)
    }

    suspend fun updateFavouritesOrder(favourites: List<Favourite>) {
        favouriteDao.updateFavourites(favourites)
    }

    suspend fun deleteAllFavourites() {
        favouriteDao.deleteAllFavourites()
    }

    suspend fun insertAllFavourites(favorites: List<Favourite>) {
        favouriteDao.insertAll(favorites)
    }

    suspend fun replaceAllFavourites(favorites: List<Favourite>) {
        favouriteDao.replaceAllFavourites(favorites)
    }

}