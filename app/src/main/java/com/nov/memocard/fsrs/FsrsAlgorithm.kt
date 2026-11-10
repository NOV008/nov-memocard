package com.nov.memocard.fsrs

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * FSRS-6 memory model, ported from open-spaced-repetition/ts-fsrs (5.4.2).
 */
class FsrsAlgorithm(val params: FsrsParameters = FsrsParameters()) {

    private val w = params.w
    private val decay = -w[20]
    private val factor = roundTo(exp(ln(0.9) / decay) - 1.0, 8)

    val intervalModifier: Double =
        roundTo((params.requestRetention.pow(1.0 / decay) - 1.0) / factor, 8)

    fun forgettingCurve(elapsedDays: Int, stability: Double): Double =
        roundTo((1.0 + factor * elapsedDays / stability).pow(decay), 8)

    fun initStability(g: Rating): Double = max(w[g.value - 1], 0.1)

    fun initDifficulty(g: Rating): Double =
        roundTo(w[4] - exp((g.value - 1) * w[5]) + 1.0, 8)

    private fun linearDamping(deltaD: Double, oldD: Double): Double =
        roundTo(deltaD * (10.0 - oldD) / 9.0, 8)

    private fun meanReversion(init: Double, current: Double): Double =
        roundTo(w[7] * init + (1.0 - w[7]) * current, 8)

    fun nextDifficulty(d: Double, g: Rating): Double {
        val deltaD = -w[6] * (g.value - 3)
        val nextD = d + linearDamping(deltaD, d)
        return clamp(meanReversion(initDifficulty(Rating.Easy), nextD), 1.0, 10.0)
    }

    fun nextRecallStability(d: Double, s: Double, r: Double, g: Rating): Double {
        val hardPenalty = if (g == Rating.Hard) w[15] else 1.0
        val easyBonus = if (g == Rating.Easy) w[16] else 1.0
        val sinc = exp(w[8]) * (11.0 - d) * s.pow(-w[9]) *
            (exp((1.0 - r) * w[10]) - 1.0) * hardPenalty * easyBonus
        return roundTo(clamp(s * (1.0 + sinc), S_MIN, S_MAX), 8)
    }

    fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        val sf = w[11] * d.pow(-w[12]) * ((s + 1.0).pow(w[13]) - 1.0) *
            exp((1.0 - r) * w[14])
        return roundTo(clamp(sf, S_MIN, S_MAX), 8)
    }

    fun nextShortTermStability(s: Double, g: Rating): Double {
        val sinc = s.pow(-w[19]) * exp(w[17] * (g.value - 3 + w[18]))
        val masked = if (g.value >= Rating.Hard.value) max(sinc, 1.0) else sinc
        return roundTo(clamp(s * masked, S_MIN, S_MAX), 8)
    }

    fun nextState(state: MemoryState, t: Int, g: Rating, r: Double? = null): MemoryState {
        val d = state.difficulty
        val s = state.stability
        if (d == 0.0 && s == 0.0) {
            return MemoryState(initStability(g), clamp(initDifficulty(g), 1.0, 10.0))
        }
        val rr = r ?: forgettingCurve(t, s)
        val newS = when {
            t == 0 && params.enableShortTerm -> nextShortTermStability(s, g)
            g == Rating.Again -> {
                val sFail = nextForgetStability(d, s, rr)
                val sMin = if (params.enableShortTerm) s / exp(w[17] * w[18]) else s
                clamp(roundTo(sMin, 8), S_MIN, sFail)
            }
            else -> nextRecallStability(d, s, rr, g)
        }
        return MemoryState(newS, nextDifficulty(d, g))
    }

    fun nextInterval(s: Double, elapsedDays: Int): Int {
        val ivl = min(
            max(1.0, Math.round(s * intervalModifier).toDouble()),
            params.maximumInterval.toDouble(),
        )
        return applyFuzz(ivl, elapsedDays)
    }

    private fun applyFuzz(ivl: Double, elapsedDays: Int): Int {
        if (!params.enableFuzz || ivl < 2.5) return Math.round(ivl).toInt()
        val (minIvl, maxIvl) = fuzzRange(ivl, elapsedDays)
        return Random.nextInt(minIvl, maxIvl + 1)
    }

    private fun fuzzRange(interval: Double, elapsedDays: Int): Pair<Int, Int> {
        var delta = 1.0
        delta += 0.15 * max(min(interval, 7.0) - 2.5, 0.0)
        delta += 0.10 * max(min(interval, 20.0) - 7.0, 0.0)
        delta += 0.05 * max(interval - 20.0, 0.0)
        val ivl = min(interval, params.maximumInterval.toDouble())
        var minIvl = max(2, Math.round(ivl - delta).toInt())
        val maxIvl = min(Math.round(ivl + delta).toInt(), params.maximumInterval)
        if (ivl > elapsedDays) minIvl = max(minIvl, elapsedDays + 1)
        minIvl = min(minIvl, maxIvl)
        return minIvl to maxIvl
    }

    companion object {
        const val S_MIN = 0.001
        const val S_MAX = 36500.0

        fun clamp(value: Double, lo: Double, hi: Double): Double = min(max(value, lo), hi)

        fun roundTo(num: Double, decimals: Int): Double {
            val f = 10.0.pow(decimals)
            return Math.round(num * f) / f
        }
    }
}