package by.grinich.hscontrol.utils

import by.grinich.hscontrol.data.FocusItem
import by.grinich.hscontrol.data.MonthlySettings
import by.grinich.hscontrol.data.PremiumItem
import by.grinich.hscontrol.data.PremiumResult
import by.grinich.hscontrol.data.PremiumSettings
import by.grinich.hscontrol.data.SalaryResult
import by.grinich.hscontrol.data.ShareResult

object Calculations {

    private val targetShares = mapOf(
        "5 Element" to listOf(50, 38, 28, 25, 19, 17, 16),
        "A1" to listOf(40, 35, 28, 22, 21, 20, 18),
        "MTS" to listOf(48, 38, 32, 25, 23, 20, 17),
        "Electrosila" to listOf(65, 55, 42, 33, 30, 25, 25)
    )
    private val targetBonuses = mapOf(
        "5 Element" to listOf(700, 700, 750, 800, 800, 900, 900),
        "A1" to listOf(700, 700, 750, 800, 800, 900, 900),
        "MTS" to listOf(700, 700, 750, 800, 800, 900, 900),
        "Electrosila" to listOf(700, 700, 750, 800, 800, 900, 900)
    )
    private val overBonuses = listOf(10, 15, 20, 30, 45, 50, 80)

    private val premTarget1 = mapOf(
        "5 Element" to 20.0,
        "A1" to 28.0,
        "MTS" to 30.0,
        "Electrosila" to 27.0
    )
    private val premTarget2 = mapOf(
        "5 Element" to 30.0,
        "A1" to 40.0,
        "MTS" to 42.0,
        "Electrosila" to 35.0
    )

    private fun clampComp(c: Int) = c.coerceIn(0, 6)

    fun getTargetShare(channel: String, competitors: Int): Int {
        val list = targetShares[channel] ?: targetShares["5 Element"]!!
        return list[clampComp(competitors)]
    }

    fun getTargetBonus(channel: String, competitors: Int): Double {
        val list = targetBonuses[channel] ?: targetBonuses["5 Element"]!!
        return list[clampComp(competitors)].toDouble()
    }

    fun getOverBonus(competitors: Int): Double {
        return overBonuses[clampComp(competitors)].toDouble()
    }

    fun getPremTar1(channel: String): Double = premTarget1[channel] ?: 20.0
    fun getPremTar2(channel: String): Double = premTarget2[channel] ?: 30.0

    fun calculateShare(
        totalHonor: Int,
        totalAll: Int,
        channel: String,
        competitors: Int,
        targetBonusOverride: Double?,
        overBonusOverride: Double?
    ): ShareResult {
        val currentShare = if (totalAll > 0) Math.round(totalHonor * 100.0 / totalAll).toInt() else 0
        val targetShare = getTargetShare(channel, competitors)
        val targetBonus = targetBonusOverride ?: getTargetBonus(channel, competitors)
        val overBonus = overBonusOverride ?: getOverBonus(competitors)

        var shareDiff = 0
        var overTotal = 0.0
        var sharePayment = 0.0

        if (currentShare > 0 && targetShare > 0) {
            if (currentShare >= targetShare) {
                shareDiff = currentShare - targetShare
                overTotal = shareDiff * overBonus
                sharePayment = targetBonus + overTotal
            } else {
                sharePayment = currentShare.toDouble() / targetShare * targetBonus
            }
        }

        return ShareResult(
            totalHonor = totalHonor,
            totalAll = totalAll,
            currentShare = currentShare,
            targetShare = targetShare,
            targetBonus = targetBonus,
            overBonus = overBonus,
            shareDiff = shareDiff,
            overTotal = overTotal,
            sharePayment = sharePayment
        )
    }

    fun calculatePremium(
        items: List<PremiumItem>,
        totalHonor: Int,
        totalAll: Int,
        channel: String,
        competitors: Int,
        settings: PremiumSettings?
    ): PremiumResult {
        val points = items.sumOf { it.rate * it.sold }
        val fixedPay = items.sumOf { it.fixedBonus * it.sold }

        val sharePercent = getTargetShare(channel, competitors).toDouble()
        val requiredHonor = totalAll * (sharePercent / 100.0)
        val percent = if (requiredHonor > 0) (points / requiredHonor) * 100 else 0.0

        val bonus1 = settings?.bonus1 ?: 400.0
        val bonus2 = settings?.bonus2 ?: 500.0
        val target1Percent = settings?.target1 ?: getPremTar1(channel)
        val target2Percent = settings?.target2 ?: getPremTar2(channel)
        val extraPerPoint = settings?.extraPerPercent ?: 10.0

        val tar1Points = requiredHonor * (target1Percent / 100.0)
        val tar2Points = requiredHonor * (target2Percent / 100.0)

        var payout = 0.0
        var extra = 0.0
        var tier = 0

        if (points > 0 && requiredHonor > 0) {
            if (points >= tar2Points) {
                extra = (points - tar2Points) * extraPerPoint
                payout = bonus2 + extra
                tier = 2
            } else if (points >= tar1Points) {
                extra = (points - tar1Points) * extraPerPoint
                payout = bonus1 + extra
                tier = 1
            } else {
                payout = if (tar1Points > 0) bonus1 * (points / tar1Points) else 0.0
                tier = 0
            }
        }

        payout += fixedPay

        return PremiumResult(
            points = points,
            totalHonor = totalHonor,
            percent = percent,
            target1 = target1Percent,
            target2 = target2Percent,
            bonus1 = bonus1,
            bonus2 = bonus2,
            extraPay = extra,
            fixedPay = fixedPay,
            payout = payout,
            tier = tier,
            requiredHonor = requiredHonor,
            sharePercent = sharePercent,
            totalAll = totalAll,
            channel = channel,
            competitors = competitors
        )
    }

    fun calculateIot(settings: MonthlySettings): Double {
        val isOM = settings.channel == "5 Element" || settings.channel == "Electrosila"
        val percentages = if (isOM) listOf(1.0, 1.0, 2.0, 2.0, 2.0) else listOf(1.5, 1.5, 3.0, 3.0, 3.0)
        val amounts = listOf(settings.iotLaptop, settings.iotPad, settings.iotWatch, settings.iotEarbuds, settings.iotOther)
        return amounts.mapIndexed { idx, amount -> amount * percentages[idx] / 100.0 }.sum()
    }

    fun calculateFocus(items: List<FocusItem>): Double {
        return items.sumOf { it.bonus * it.sold }
    }

    fun calculateSalary(
        version: Int,
        settings: MonthlySettings,
        sharePayment: Double,
        premiumPayout: Double,
        iotBonus: Double,
        focusBonus: Double
    ): SalaryResult {
        val base = settings.baseSalary
        val mobile = settings.mobilePayment
        val testBonus = if (settings.testPassed) settings.testBonus else 0.0
        val advance = settings.advanceAmount

        val total = base + mobile + sharePayment + premiumPayout + iotBonus + focusBonus + testBonus
        val final = Math.max(0.0, total - advance)

        return SalaryResult(
            version = version,
            baseSalary = base,
            mobilePayment = mobile,
            sharePayment = sharePayment,
            premiumPayout = premiumPayout,
            iotBonus = iotBonus,
            focusBonus = focusBonus,
            testBonus = testBonus,
            totalSalary = total,
            advanceAmount = advance,
            finalPayment = final
        )
    }
}