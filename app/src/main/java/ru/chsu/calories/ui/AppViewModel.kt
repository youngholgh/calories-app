package ru.chsu.calories.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.chsu.calories.data.Competition
import ru.chsu.calories.data.DayLog
import ru.chsu.calories.data.DayResult
import ru.chsu.calories.data.DemoData
import ru.chsu.calories.data.DemoRivals
import ru.chsu.calories.data.FoodEntry
import ru.chsu.calories.data.Meal
import ru.chsu.calories.data.Nutrients
import ru.chsu.calories.data.Participant
import ru.chsu.calories.data.Product
import ru.chsu.calories.data.ProductCatalog
import ru.chsu.calories.data.Profile
import ru.chsu.calories.data.Standing
import ru.chsu.calories.data.local.AppDatabase
import ru.chsu.calories.data.local.LocalRepository
import ru.chsu.calories.domain.Norm
import ru.chsu.calories.domain.NormCalculator
import ru.chsu.calories.domain.Scoring
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Сообщение для Snackbar. id нужен, чтобы одинаковый текст показывался повторно. */
data class UiMessage(val text: String, val id: Long = System.nanoTime())

/**
 * ViewModel (MVVM): хранит состояние приложения и всю логику.
 * Данные сохраняются в локальную базу Room и переживают закрытие приложения.
 * Следующий шаг — синхронизация с сервером (Retrofit + REST API).
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LocalRepository(AppDatabase.get(app).dao())
    private val saveLock = Mutex()

    val today: LocalDate get() = LocalDate.now()

    /** false, пока данные читаются из базы при запуске. */
    var loaded by mutableStateOf(false)
        private set

    var userEmail by mutableStateOf<String?>(null)
        private set
    var profile by mutableStateOf(Profile())
        private set
    var customProducts by mutableStateOf(listOf<Product>())
        private set
    var days by mutableStateOf(mapOf<LocalDate, DayLog>())
        private set
    var selectedDate by mutableStateOf(LocalDate.now())
        private set
    var competition by mutableStateOf<Competition?>(null)
        private set

    // имитация синхронизации с сервером
    var online by mutableStateOf(true)
        private set
    var syncQueue by mutableIntStateOf(0)
        private set

    var message by mutableStateOf<UiMessage?>(null)
    var lastCustomProductId by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            val saved = repo.load()
            userEmail = saved.email
            saved.profile?.let { profile = it }
            customProducts = saved.customProducts
            competition = saved.competition
            if (saved.days.isEmpty()) {
                // первый запуск — заполняем дневник демо-данными и сразу сохраняем
                days = DemoData.history(today)
                persist { repo.replaceDiary(days) }
            } else {
                days = saved.days
            }
            loaded = true
        }
    }

    /** Сохранения выполняются строго по очереди, чтобы старые данные не перезаписали новые. */
    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { saveLock.withLock { block() } }
    }

    // ---------- чтение ----------

    val norm: Norm get() = NormCalculator.calculate(profile)

    val allProducts: List<Product> get() = ProductCatalog.products + customProducts

    fun product(id: String): Product = allProducts.firstOrNull { it.id == id } ?: ProductCatalog.unknown

    fun day(date: LocalDate = selectedDate): DayLog = days[date] ?: DayLog()

    fun nutrients(e: FoodEntry): Nutrients = Nutrients.of(product(e.productId), e.grams)

    fun total(list: List<FoodEntry>): Nutrients = list.fold(Nutrients()) { acc, e -> acc + nutrients(e) }

    fun dayTotal(date: LocalDate = selectedDate): Nutrients = total(day(date).meals.values.flatten())

    fun search(query: String, source: List<Product> = allProducts): List<Product> {
        val q = query.trim()
        return if (q.isEmpty()) source else source.filter { it.name.contains(q, ignoreCase = true) }
    }

    // ---------- действия ----------

    fun show(text: String) {
        message = UiMessage(text)
    }

    /** Возвращает текст ошибки или null, если всё хорошо. */
    fun login(email: String, password: String, register: Boolean): String? {
        if (!Regex("^\\S+@\\S+\\.\\S+$").matches(email)) return "Введите почту в формате name@mail.ru"
        if (password.length < 6) return "Пароль должен быть не короче 6 символов"
        userEmail = email
        persist { repo.saveProfile(email, profile) }
        show(if (register) "Аккаунт создан" else "Вход выполнен")
        return null
    }

    fun logout() {
        userEmail = null
        persist { repo.saveProfile(null, profile) }
    }

    fun updateProfile(p: Profile) {
        profile = p
        val email = userEmail
        persist { repo.saveProfile(email, p) }
    }

    private fun editDay(date: LocalDate, change: (DayLog) -> DayLog) {
        val updated = change(day(date))
        days = days + (date to updated)
        if (!online) syncQueue++
        persist { repo.saveDay(date, updated) }
    }

    fun addEntry(meal: Meal, productId: String, grams: Int) {
        editDay(selectedDate) { d ->
            d.copy(meals = d.meals + (meal to d.meals[meal].orEmpty() + FoodEntry(productId, grams)))
        }
        val p = product(productId)
        show("${p.name} → ${meal.title}: +${(p.kcal * grams / 100).roundToInt()} ккал")
    }

    fun updateEntry(meal: Meal, index: Int, grams: Int) {
        editDay(selectedDate) { d ->
            val list = d.meals[meal].orEmpty().toMutableList()
            if (index in list.indices) list[index] = list[index].copy(grams = grams)
            d.copy(meals = d.meals + (meal to list))
        }
        show("Масса изменена")
    }

    fun removeEntry(meal: Meal, index: Int) {
        val removed = day().meals[meal].orEmpty().getOrNull(index) ?: return
        editDay(selectedDate) { d ->
            val list = d.meals[meal].orEmpty().toMutableList()
            list.removeAt(index)
            d.copy(meals = d.meals + (meal to list))
        }
        show("${product(removed.productId).name} — удалено")
    }

    fun toggleWater(index: Int) {
        editDay(selectedDate) { d ->
            val n = if (d.waterGlasses == index + 1) index else index + 1
            d.copy(waterGlasses = n)
        }
    }

    fun shiftDate(daysDelta: Long) {
        val d = selectedDate.plusDays(daysDelta)
        if (!d.isAfter(today)) selectedDate = d
    }

    fun openDate(date: LocalDate) {
        selectedDate = date
    }

    fun toggleOnline() {
        online = !online
        if (online) {
            show(if (syncQueue > 0) "Сеть есть. Отправлено на сервер: $syncQueue" else "Сеть есть, данные актуальны")
            syncQueue = 0
        } else {
            show("Режим без сети: изменения сохранятся локально")
        }
    }

    fun addCustomProduct(name: String, kcal: Double, protein: Double, fat: Double, carbs: Double, category: Int): Product {
        val p = Product("c${System.currentTimeMillis()}", name, kcal, protein, fat, carbs, category, custom = true)
        customProducts = customProducts + p
        lastCustomProductId = p.id
        if (!online) syncQueue++
        persist { repo.saveCustomProduct(p) }
        show("Продукт «$name» добавлен в справочник")
        return p
    }

    fun resetDiary() {
        days = DemoData.history(today)
        selectedDate = today
        val snapshot = days
        persist { repo.replaceDiary(snapshot) }
        show("Дневник заполнен демо-данными заново")
    }

    // ---------- соревнование ----------

    /** Дни текущей недели с понедельника по сегодня — по ним считается рейтинг. */
    fun weekDates(): List<LocalDate> {
        val monday = today.with(DayOfWeek.MONDAY)
        val count = ChronoUnit.DAYS.between(monday, today)
        return (0..count).map { monday.plusDays(it) }
    }

    fun standings(): List<Standing> {
        val c = competition ?: return emptyList()
        val dates = weekDates()
        val me = Participant(
            id = "me",
            name = "Вы",
            isMe = true,
            normKcal = norm.kcal,
            days = dates.associateWith { DayResult(dayTotal(it).kcal, day(it).waterGlasses) }
        )
        val everyone = listOf(me) + DemoRivals.generate(c.code, dates, today)
        return Scoring.rank(everyone.map { Scoring.standing(it, dates) })
    }

    fun createCompetition(name: String) {
        val code = "KAL-" + (1000..9999).random()
        val c = Competition(name.trim().ifBlank { "Неделя без срывов" }, code, today)
        competition = c
        persist { repo.saveCompetition(c) }
        show("Соревнование создано. Код для друзей: $code")
    }

    /** Возвращает текст ошибки или null. */
    fun joinCompetition(rawCode: String): String? {
        val code = rawCode.trim().uppercase()
        if (!Regex("^KAL-\\d{4}$").matches(code)) return "Код выглядит так: KAL-1234"
        val c = Competition("Группа $code", code, today)
        competition = c
        persist { repo.saveCompetition(c) }
        show("Вы вступили в группу $code")
        return null
    }

    fun leaveCompetition() {
        competition = null
        persist { repo.saveCompetition(null) }
        show("Вы вышли из соревнования")
    }
}
