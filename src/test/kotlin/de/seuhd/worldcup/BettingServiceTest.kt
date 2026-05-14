package de.seuhd.worldcup

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BettingServiceTest {

    private fun match(id: Int, home: String, away: String, hs: Int?, aws: Int?) =
        Match(
            matchId = id,
            round = "Matchday 1",
            date = "2026-06-01",
            homeTeam = home,
            awayTeam = away,
            homeScore = hs,
            awayScore = aws,
            ground = "Test Stadium"
        )

    @BeforeTest
    fun resetBets() {
        BettingService.clear()
    }

    // ── evaluateBonus ──────────────────────────────────────────────────────────

    @Test
    fun `evaluateBonus awards 3 points for an exact score prediction`() {
        val matches = listOf(match(1, "AAA", "BBB", 2, 0))
        BettingService.placeBet(
            Bet(matchId = 1, prediction = Prediction.HOME_WIN, predictedHomeScore = 2, predictedAwayScore = 0)
        )

        assertEquals(3, BettingService.evaluateBonus(matches))
    }

    @Test
    fun `evaluateBonus awards 1 point for correct outcome without exact score`() {
        val matches = listOf(
            match(1, "AAA", "BBB", 3, 1),
            match(2, "AAA", "CCC", 1, 1)
        )
        // Correct outcome, wrong exact score.
        BettingService.placeBet(
            Bet(matchId = 1, prediction = Prediction.HOME_WIN, predictedHomeScore = 2, predictedAwayScore = 0)
        )
        // Correct outcome, no score prediction.
        BettingService.placeBet(Bet(matchId = 2, prediction = Prediction.DRAW))

        assertEquals(2, BettingService.evaluateBonus(matches))
    }

    @Test
    fun `evaluateBonus awards 0 points for a wrong prediction`() {
        val matches = listOf(match(1, "AAA", "BBB", 2, 0))
        BettingService.placeBet(
            Bet(matchId = 1, prediction = Prediction.AWAY_WIN, predictedHomeScore = 0, predictedAwayScore = 2)
        )

        assertEquals(0, BettingService.evaluateBonus(matches))
    }

    @Test
    fun `evaluateBonus ignores unplayed matches`() {
        val matches = listOf(match(1, "AAA", "BBB", null, null))
        BettingService.placeBet(
            Bet(matchId = 1, prediction = Prediction.HOME_WIN, predictedHomeScore = 2, predictedAwayScore = 0)
        )

        assertEquals(0, BettingService.evaluateBonus(matches))
    }

    // ── removeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `removeBet removes an existing bet so it no longer affects evaluation`() {
        val matches = listOf(
            match(1, "AAA", "BBB", 2, 0),
            match(2, "AAA", "CCC", 1, 1)
        )
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN))
        BettingService.placeBet(Bet(2, Prediction.DRAW))

        BettingService.removeBet(1)

        val result = BettingService.evaluate(matches)
        assertEquals(1, result.evaluated)
        assertEquals(1, result.correct)
    }

    @Test
    fun `removeBet does nothing when no bet exists for that matchId`() {
        val matches = listOf(match(1, "AAA", "BBB", 2, 0))
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN))

        // Removing a non-existing matchId must not throw and must not affect existing bets.
        BettingService.removeBet(999)

        val result = BettingService.evaluate(matches)
        assertEquals(1, result.evaluated)
        assertEquals(1, result.correct)
    }

    // ── changeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `changeBet updates the prediction for an existing bet`() {
        val matches = listOf(match(1, "AAA", "BBB", 2, 0))
        BettingService.placeBet(Bet(1, Prediction.AWAY_WIN))

        BettingService.changeBet(Bet(1, Prediction.HOME_WIN))

        val result = BettingService.evaluate(matches)
        assertEquals(1, result.evaluated)
        assertEquals(1, result.correct)
    }

    @Test
    fun `changeBet throws when no bet exists for that matchId`() {
        assertFailsWith<IllegalArgumentException> {
            BettingService.changeBet(Bet(42, Prediction.HOME_WIN))
        }
    }
}
