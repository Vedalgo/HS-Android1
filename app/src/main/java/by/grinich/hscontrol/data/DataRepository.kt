package by.grinich.hscontrol.data

import by.grinich.hscontrol.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object DataRepository {

    private val client get() = SupabaseClientProvider.client

    suspend fun getSales(userId: String, month: String): List<Sale> {
        val start = "$month-01"
        val end = "$month-31"
        val sales = client.postgrest["sales"]
            .select {
                filter {
                    eq("user_id", userId)
                    gte("sale_date", start)
                    lte("sale_date", end)
                }
                order("sale_date", Order.DESCENDING)
            }
            .decodeList<Sale>()
        return sales.distinctBy { it.id }
    }

    suspend fun addSale(sale: NewSale) {
        client.postgrest["sales"].insert(sale)
    }

    suspend fun addSales(sales: List<NewSale>) {
        client.postgrest["sales"].insert(sales)
    }

    suspend fun deleteSale(saleId: String) {
        client.postgrest["sales"].delete {
            filter { eq("id", saleId) }
        }
    }

    suspend fun getMonthlySettings(userId: String, month: String): MonthlySettings? {
        return try {
            client.postgrest["monthly_settings"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("month", month)
                    }
                }
                .decodeSingleOrNull<MonthlySettings>()
        } catch (e: Exception) { null }
    }

    suspend fun upsertMonthlySettings(settings: MonthlySettings) {
        try {
            client.postgrest.rpc(
                function = "upsert_monthly_settings",
                parameters = buildJsonObject {
                    put("p_user_id", settings.userId)
                    put("p_month", settings.month)
                    put("p_channel", settings.channel)
                    put("p_competitors", settings.competitorsCount)
                    put("p_base_salary", settings.baseSalary)
                    put("p_mobile_payment", settings.mobilePayment)
                    put("p_target_bonus", settings.targetBonus)
                    put("p_over_bonus", settings.overBonus)
                    put("p_test_bonus", settings.testBonus)
                    put("p_test_passed", settings.testPassed)
                    put("p_advance_amount", settings.advanceAmount)
                    put("p_adjusted_salary", settings.adjustedSalary)
                    put("p_iot_laptop", settings.iotLaptop)
                    put("p_iot_pad", settings.iotPad)
                    put("p_iot_watch", settings.iotWatch)
                    put("p_iot_earbuds", settings.iotEarbuds)
                    put("p_iot_other", settings.iotOther)
                    put("p_plan_quantity", settings.planQuantity)
                    put("p_overplan_bonus", settings.overplanBonus)
                    put("p_base_plan_payment", settings.basePlanPayment)
                }
            )
        } catch (e: Exception) {
            throw Exception("Не удалось сохранить настройки: ${e.message}")
        }
    }

    suspend fun getPremiumItems(userId: String, month: String): List<PremiumItem> {
        return client.postgrest["premium_items"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("month", month)
                }
            }
            .decodeList<PremiumItem>()
    }

    suspend fun addPremiumItem(item: NewPremiumItem) {
        client.postgrest["premium_items"].insert(item)
    }

    suspend fun updatePremiumSold(id: String, sold: Int) {
        client.postgrest["premium_items"].update(mapOf("sold" to sold)) {
            filter { eq("id", id) }
        }
    }

    suspend fun updatePremiumRate(id: String, rate: Double) {
        client.postgrest["premium_items"].update(mapOf("rate" to rate)) {
            filter { eq("id", id) }
        }
    }

    suspend fun updatePremiumFixed(id: String, fixedBonus: Double) {
        client.postgrest["premium_items"].update(mapOf("fixed_bonus" to fixedBonus)) {
            filter { eq("id", id) }
        }
    }

    suspend fun deletePremiumItem(id: String) {
        client.postgrest["premium_items"].delete {
            filter { eq("id", id) }
        }
    }

    suspend fun getPremiumSettings(userId: String, month: String): PremiumSettings? {
        return try {
            client.postgrest["premium_settings"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("month", month)
                    }
                }
                .decodeSingleOrNull<PremiumSettings>()
        } catch (e: Exception) { null }
    }

    suspend fun upsertPremiumSettings(settings: PremiumSettings) {
        try {
            client.postgrest.rpc(
                function = "upsert_premium_settings",
                parameters = buildJsonObject {
                    put("p_user_id", settings.userId)
                    put("p_month", settings.month)
                    put("p_bonus1", settings.bonus1)
                    put("p_bonus2", settings.bonus2)
                    put("p_target1", settings.target1)
                    put("p_target2", settings.target2)
                    put("p_extra_per_percent", settings.extraPerPercent)
                }
            )
        } catch (e: Exception) {
            throw Exception("Не удалось сохранить Premium: ${e.message}")
        }
    }

    suspend fun getFocusItems(userId: String, month: String): List<FocusItem> {
        return client.postgrest["focus_items"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("month", month)
                }
            }
            .decodeList<FocusItem>()
    }

    suspend fun addFocusItem(item: NewFocusItem) {
        client.postgrest["focus_items"].insert(item)
    }

    suspend fun updateFocusSold(id: String, sold: Int) {
        client.postgrest["focus_items"].update(mapOf("sold" to sold)) {
            filter { eq("id", id) }
        }
    }

    suspend fun updateFocusBonus(id: String, bonus: Double) {
        client.postgrest["focus_items"].update(mapOf("bonus" to bonus)) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteFocusItem(id: String) {
        client.postgrest["focus_items"].delete {
            filter { eq("id", id) }
        }
    }

    suspend fun getHonorModels(): List<HonorModelDto> {
        return client.postgrest["honor_models"]
            .select { order("sort_order", Order.ASCENDING) }
            .decodeList<HonorModelDto>()
    }

    suspend fun addHonorModel(name: String, sortOrder: Int) {
        client.postgrest["honor_models"].insert(
            mapOf("model_name" to name, "sort_order" to sortOrder)
        )
    }

    suspend fun deleteHonorModel(id: String) {
        client.postgrest["honor_models"].delete {
            filter { eq("id", id) }
        }
    }

    suspend fun swapHonorModels(idA: String, orderA: Int, idB: String, orderB: Int) {
        try {
            client.postgrest["honor_models"].update(mapOf("sort_order" to orderB)) {
                filter { eq("id", idA) }
            }
            client.postgrest["honor_models"].update(mapOf("sort_order" to orderA)) {
                filter { eq("id", idB) }
            }
        } catch (e: Exception) {
            throw Exception("Не удалось изменить порядок: ${e.message}")
        }
    }

    suspend fun calculateSalary(userId: String, month: String): SalaryResult {
        return client.postgrest.rpc(
            function = "calculate_salary",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_month", month)
            }
        ).decodeSingle<SalaryResult>()
    }

    suspend fun calculateShare(userId: String, month: String): ShareResult {
        return client.postgrest.rpc(
            function = "calculate_share",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_month", month)
            }
        ).decodeSingle<ShareResult>()
    }

    suspend fun calculatePremium(userId: String, month: String): PremiumResult {
        return client.postgrest.rpc(
            function = "calculate_premium",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_month", month)
            }
        ).decodeSingle<PremiumResult>()
    }

    suspend fun getPromotersStats(month: String): List<PromoterStats> {
        return client.postgrest.rpc(
            function = "get_promoters_stats",
            parameters = buildJsonObject {
                put("p_month", month)
            }
        ).decodeList<PromoterStats>()
    }

    suspend fun getMonthlyReport(userId: String, month: String): MonthlyReport {
        return client.postgrest.rpc(
            function = "get_monthly_report",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_month", month)
            }
        ).decodeSingle<MonthlyReport>()
    }

    suspend fun getSalesInRange(
        userId: String,
        fromDate: String,
        toDate: String
    ): List<Sale> {
        return client.postgrest["sales"]
            .select {
                filter {
                    eq("user_id", userId)
                    gte("sale_date", fromDate)
                    lte("sale_date", toDate)
                }
                order("sale_date", Order.DESCENDING)
            }
            .decodeList<Sale>()
    }

    suspend fun getFeatureFlags(): List<FeatureFlag> {
        return client.postgrest["feature_flags"]
            .select()
            .decodeList<FeatureFlag>()
    }

    suspend fun setFeatureFlag(key: String, enabled: Boolean) {
        client.postgrest.rpc(
            function = "set_feature_flag",
            parameters = buildJsonObject {
                put("p_key", key)
                put("p_enabled", enabled)
            }
        )
    }
}