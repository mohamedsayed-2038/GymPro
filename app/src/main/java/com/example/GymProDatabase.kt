package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Meal Macro State
@Entity(tableName = "macro_progress")
data class MacroProgress(
    @PrimaryKey val id: Int = 1,
    val isGymDay: Boolean = true,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,

    // User profile fields (0 / empty means not set)
    val username: String = "",
    val weight: Float = 0f,
    val height: Float = 0f,
    val age: Int = 0,
    val gender: String = "", // Male / Female
    val activityLevel: String = "", // Sedentary, Moderate, Active, Elite Athlete
    val fitnessGoal: String = "", // Bulking, Cutting, Recomp
    val injuries: String = "",
    val dietType: String = "",

    // Dynamic targets calculated by AI or mathematical formulas
    val targetCaloriesGym: Int = 0,
    val targetCaloriesRest: Int = 0,
    val targetProteinGym: Int = 0,
    val targetProteinRest: Int = 0,
    val targetCarbsGym: Int = 0,
    val targetCarbsRest: Int = 0,
    val targetFatsGym: Int = 0,
    val targetFatsRest: Int = 0
)

data class MacroTargets(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int
)

fun MacroProgress.getTargets(): MacroTargets {
    val isGym = this.isGymDay
    val hasCustomTargets = this.targetCaloriesGym > 0
    
    return if (hasCustomTargets) {
        MacroTargets(
            calories = if (isGym) this.targetCaloriesGym else this.targetCaloriesRest,
            protein = if (isGym) this.targetProteinGym else this.targetProteinRest,
            carbs = if (isGym) this.targetCarbsGym else this.targetCarbsRest,
            fats = if (isGym) this.targetFatsGym else this.targetFatsRest
        )
    } else {
        MacroTargets(
            calories = if (isGym) 2800 else 2200,
            protein = if (isGym) 180 else 140,
            carbs = if (isGym) 300 else 200,
            fats = 70
        )
    }
}

// Workout Set State
@Entity(tableName = "workout_progress")
data class WorkoutProgress(
    @PrimaryKey val exerciseId: String,
    val set1Done: Boolean = false,
    val set2Done: Boolean = false,
    val set3Done: Boolean = false
)

@Dao
interface GymDao {
    @Query("SELECT * FROM macro_progress WHERE id = 1")
    fun getMacros(): Flow<MacroProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMacros(macros: MacroProgress)

    @Query("SELECT * FROM workout_progress")
    fun getAllWorkouts(): Flow<List<WorkoutProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWorkout(progress: WorkoutProgress)
    
    @Query("DELETE FROM workout_progress")
    suspend fun resetWorkouts()
}

@Database(entities = [MacroProgress::class, WorkoutProgress::class], version = 2, exportSchema = false)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
