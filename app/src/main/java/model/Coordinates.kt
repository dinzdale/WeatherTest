package model

import android.arch.persistence.room.ColumnInfo

/**
 * Created by gjacobs on 10/30/15.
 */
data class Coordinates(val void: Unit? = null) {
    var lat: Double = 0.0
    var lon: Double = 0.0
}

