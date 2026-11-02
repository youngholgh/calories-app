package ru.chsu.calories.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
abstract class CaloriesDao {

    // ---------- профиль ----------
    @Query("SELECT * FROM profile WHERE id = 1")
    abstract suspend fun profile(): ProfileEntity?

    @Upsert
    abstract suspend fun saveProfile(profile: ProfileEntity)

    // ---------- дневник ----------
    @Query("SELECT * FROM diary_entries ORDER BY day, meal, position")
    abstract suspend fun allEntries(): List<DiaryEntryEntity>

    @Query("SELECT * FROM water")
    abstract suspend fun allWater(): List<WaterEntity>

    @Query("DELETE FROM diary_entries WHERE day = :day")
    abstract suspend fun clearDay(day: String)

    @Insert
    abstract suspend fun insertEntries(entries: List<DiaryEntryEntity>)

    @Upsert
    abstract suspend fun saveWater(water: WaterEntity)

    /** День сохраняется целиком: старые записи удаляются, новые вставляются. */
    @Transaction
    open suspend fun replaceDay(day: String, entries: List<DiaryEntryEntity>, water: WaterEntity) {
        clearDay(day)
        insertEntries(entries)
        saveWater(water)
    }

    @Query("DELETE FROM diary_entries")
    abstract suspend fun clearAllEntries()

    @Query("DELETE FROM water")
    abstract suspend fun clearAllWater()

    @Transaction
    open suspend fun replaceDiary(entries: List<DiaryEntryEntity>, water: List<WaterEntity>) {
        clearAllEntries()
        clearAllWater()
        insertEntries(entries)
        water.forEach { saveWater(it) }
    }

    // ---------- свои продукты ----------
    @Query("SELECT * FROM custom_products")
    abstract suspend fun customProducts(): List<CustomProductEntity>

    @Upsert
    abstract suspend fun saveCustomProduct(product: CustomProductEntity)

    // ---------- соревнование ----------
    @Query("SELECT * FROM competition WHERE id = 1")
    abstract suspend fun competition(): CompetitionEntity?

    @Upsert
    abstract suspend fun saveCompetition(competition: CompetitionEntity)

    @Query("DELETE FROM competition")
    abstract suspend fun deleteCompetition()
}
