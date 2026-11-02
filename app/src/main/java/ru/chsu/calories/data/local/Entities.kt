package ru.chsu.calories.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * Таблицы локальной базы (Room). Даты хранятся строкой ISO ("2026-11-01"),
 * перечисления — их именем, поэтому TypeConverter не нужен.
 */

/** Профиль и вход — всегда одна строка с id = 1. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val email: String?,
    val sex: String,
    val age: Int,
    val heightCm: Int,
    val weightKg: Int,
    val activity: String,
    val goal: String
)

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: String,
    val meal: String,
    val position: Int,
    val productId: String,
    val grams: Int
)

@Entity(tableName = "water")
data class WaterEntity(
    @PrimaryKey val day: String,
    val glasses: Int
)

@Entity(tableName = "custom_products")
data class CustomProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val category: Int
)

/** Текущее соревнование — тоже одна строка. Участники восстанавливаются по коду. */
@Entity(tableName = "competition")
data class CompetitionEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val code: String,
    val startDay: String
)
