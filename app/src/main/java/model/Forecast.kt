package model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by gjacobs on 10/31/15.
 */
@Entity(tableName = "forecast", primaryKeys = arrayOf("lattitude", "longitude"))
data class Forecast(val void: Unit? = null) {
    var lattitude: Double = 0.0
    var longitude: Double = 0.0
    @Embedded
    lateinit var city: City
    lateinit var cod: String
    var message: Float = 0.0F
    var cnt: Int = 0
    lateinit var list: ArrayList<ForecastDetails>
}
