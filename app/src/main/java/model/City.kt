package model

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.PrimaryKey

/**
 * Created by gjacobs on 10/31/15.
 */
data class City(val void : Unit?=null) {
    var id: Int = 0
    lateinit var name: String
    @Embedded
    lateinit var coord: Coordinates
    lateinit var country: String
    var population: Int = 0
}
