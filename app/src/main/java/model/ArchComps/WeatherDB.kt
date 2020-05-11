package model.ArchComps


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import model.CurrentWeather

/**
 * Created by garyjacobs on 1/10/18.
 */
@Database(entities = arrayOf(CurrentWeather::class), version = 1)
@TypeConverters(Converters::class)
abstract class WeatherDB : RoomDatabase() {
    companion object {
        private var instance: WeatherDB? = null
        fun getInstance(context: Context): WeatherDB? {
            if (instance == null) {
                instance = Room.databaseBuilder(context, WeatherDB::class.java, WeatherDB::class.java.simpleName)
                        .build()
            }
            return instance
        }
    }

    abstract fun weatherDao(): WeatherDao
}