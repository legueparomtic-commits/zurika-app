package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromCategory(category: ServiceCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): ServiceCategory = ServiceCategory.valueOf(value)

    @TypeConverter
    fun fromServiceStatus(status: ServiceStatus): String = status.name

    @TypeConverter
    fun toServiceStatus(value: String): ServiceStatus = ServiceStatus.valueOf(value)

    @TypeConverter
    fun fromProblemType(problem: ProblemType): String = problem.name

    @TypeConverter
    fun toProblemType(value: String): ProblemType = ProblemType.valueOf(value)

    @TypeConverter
    fun fromRequestStatus(status: RequestStatus): String = status.name

    @TypeConverter
    fun toRequestStatus(value: String): RequestStatus = RequestStatus.valueOf(value)
}

@Database(
    entities = [
        UserEntity::class,
        ServiceEntity::class,
        InterventionEntity::class,
        ReviewEntity::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ZurikaDatabase : RoomDatabase() {

    abstract fun zurikaDao(): ZurikaDao

    companion object {
        @Volatile
        private var INSTANCE: ZurikaDatabase? = null

        fun getDatabase(context: Context): ZurikaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZurikaDatabase::class.java,
                    "zurika_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
