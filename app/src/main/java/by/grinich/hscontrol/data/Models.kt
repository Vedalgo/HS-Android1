package by.grinich.hscontrol.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sale(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("sale_date") val saleDate: String,
    val brand: String,
    val model: String? = null,
    val quantity: Int = 1
)

@Serializable
data class NewSale(
    @SerialName("user_id") val userId: String,
    @SerialName("sale_date") val saleDate: String,
    val brand: String,
    val model: String? = null,
    val quantity: Int = 1
)

@Serializable
data class MonthlySettings(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val month: String,
    val channel: String = "5 Element",
    @SerialName("competitors_count") val competitorsCount: Int = 0,
    @SerialName("plan_quantity") val planQuantity: Int = 30,
    @SerialName("overplan_bonus") val overplanBonus: Double = 10.0,
    @SerialName("base_plan_payment") val basePlanPayment: Double = 800.0,
    @SerialName("test_bonus") val testBonus: Double = 50.0,
    @SerialName("test_passed") val testPassed: Boolean = false,
    @SerialName("advance_amount") val advanceAmount: Double = 0.0,
    @SerialName("adjusted_salary") val adjustedSalary: Double = 0.0,
    @SerialName("mobile_payment") val mobilePayment: Double = 10.0,
    @SerialName("base_salary") val baseSalary: Double = 500.0,
    @SerialName("target_bonus") val targetBonus: Double? = null,
    @SerialName("over_bonus") val overBonus: Double? = null,
    @SerialName("iot_laptop") val iotLaptop: Double = 0.0,
    @SerialName("iot_pad") val iotPad: Double = 0.0,
    @SerialName("iot_watch") val iotWatch: Double = 0.0,
    @SerialName("iot_earbuds") val iotEarbuds: Double = 0.0,
    @SerialName("iot_other") val iotOther: Double = 0.0
)

@Serializable
data class PremiumItem(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val month: String,
    val name: String,
    val rate: Double = 1.0,
    val sold: Int = 0,
    @SerialName("fixed_bonus") val fixedBonus: Double = 0.0
)

@Serializable
data class NewPremiumItem(
    @SerialName("user_id") val userId: String,
    val month: String,
    val name: String,
    val rate: Double,
    val sold: Int = 0,
    @SerialName("fixed_bonus") val fixedBonus: Double = 0.0
)

@Serializable
data class PremiumSettings(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val month: String,
    val bonus1: Double = 400.0,
    val bonus2: Double = 500.0,
    val target1: Double = 20.0,
    val target2: Double = 30.0,
    @SerialName("extra_per_percent") val extraPerPercent: Double = 10.0
)

@Serializable
data class FocusItem(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val month: String,
    val category: String = "Смартфоны",
    val name: String,
    val bonus: Double = 0.0,
    val sold: Int = 0
)

@Serializable
data class NewFocusItem(
    @SerialName("user_id") val userId: String,
    val month: String,
    val category: String,
    val name: String,
    val bonus: Double,
    val sold: Int = 0
)

@Serializable
data class HonorModelDto(
    val id: String,
    @SerialName("model_name") val modelName: String,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class SalaryResult(
    val version: Int = 1,
    @SerialName("base_salary") val baseSalary: Double = 500.0,
    @SerialName("mobile_payment") val mobilePayment: Double = 10.0,
    @SerialName("share_payment") val sharePayment: Double = 0.0,
    @SerialName("premium_payout") val premiumPayout: Double = 0.0,
    @SerialName("iot_bonus") val iotBonus: Double = 0.0,
    @SerialName("focus_bonus") val focusBonus: Double = 0.0,
    @SerialName("test_bonus") val testBonus: Double = 0.0,
    @SerialName("total_salary") val totalSalary: Double = 0.0,
    @SerialName("advance_amount") val advanceAmount: Double = 0.0,
    @SerialName("final_payment") val finalPayment: Double = 0.0
)

@Serializable
data class ShareResult(
    @SerialName("total_honor") val totalHonor: Int = 0,
    @SerialName("total_all") val totalAll: Int = 0,
    @SerialName("current_share") val currentShare: Int = 0,
    @SerialName("target_share") val targetShare: Int = 0,
    @SerialName("target_bonus") val targetBonus: Double = 0.0,
    @SerialName("over_bonus") val overBonus: Double = 0.0,
    @SerialName("share_diff") val shareDiff: Int = 0,
    @SerialName("over_total") val overTotal: Double = 0.0,
    @SerialName("share_payment") val sharePayment: Double = 0.0
)

@Serializable
data class PremiumResult(
    val points: Double = 0.0,
    @SerialName("total_honor") val totalHonor: Int = 0,
    val percent: Double = 0.0,
    val target1: Double = 20.0,
    val target2: Double = 30.0,
    val bonus1: Double = 400.0,
    val bonus2: Double = 500.0,
    @SerialName("extra_pay") val extraPay: Double = 0.0,
    @SerialName("fixed_pay") val fixedPay: Double = 0.0,
    val payout: Double = 0.0,
    val tier: Int = 0,
    @SerialName("required_honor") val requiredHonor: Double = 0.0,
    @SerialName("share_percent") val sharePercent: Double = 0.0,
    @SerialName("total_all") val totalAll: Int = 0,
    val channel: String = "",
    val competitors: Int = 0
)

@Serializable
data class PromoterStats(
    val id: String,
    val email: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val role: String = "promoter",
    @SerialName("supervisor_id") val supervisorId: String? = null,
    @SerialName("total_honor") val totalHonor: Int = 0,
    @SerialName("total_all") val totalAll: Int = 0,
    @SerialName("current_share") val currentShare: Int = 0,
    @SerialName("share_payment") val sharePayment: Double = 0.0,
    @SerialName("premium_payout") val premiumPayout: Double = 0.0,
    @SerialName("iot_bonus") val iotBonus: Double = 0.0,
    @SerialName("total_salary") val totalSalary: Double = 0.0,
    @SerialName("final_payment") val finalPayment: Double = 0.0
)

@Serializable
data class MonthlyReport(
    @SerialName("honor_first_half") val honorFirstHalf: Int = 0,
    @SerialName("competitors_first_half") val competitorsFirstHalf: Int = 0,
    @SerialName("honor_second_half") val honorSecondHalf: Int = 0,
    @SerialName("competitors_second_half") val competitorsSecondHalf: Int = 0,
    @SerialName("xiaomi_poco_total") val xiaomiPocoTotal: Int = 0,
    @SerialName("samsung_total") val samsungTotal: Int = 0,
    @SerialName("apple_total") val appleTotal: Int = 0,
    @SerialName("total_all") val totalAll: Int = 0,
    @SerialName("mid_month_day") val midMonthDay: Int = 15
)

@Serializable
data class FeatureFlag(
    val key: String,
    val enabled: Boolean = false,
    val title: String? = null,
    val message: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null
)