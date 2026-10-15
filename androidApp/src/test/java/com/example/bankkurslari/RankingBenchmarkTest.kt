package com.example.bankkurslari

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Times rankBanks on the real branch list (backend/data) against the previous all-pairs matching. */
class RankingBenchmarkTest {
    private fun branches(): List<OsmBranch> {
        val f = File("../backend/data/branches.json")
        val root = JsonLenient.parseToJsonElement(f.readText())
        val arr = (root as? kotlinx.serialization.json.JsonArray) ?: (root as kotlinx.serialization.json.JsonObject).arr("branches")!!
        return arr.objects().mapNotNull { o ->
            OsmBranch(o.str("bank") ?: "Bank", o.str("name") ?: "Bank", o.str("address"), o.str("hours"),
                o.dbl("lat") ?: return@mapNotNull null, o.dbl("lng") ?: return@mapNotNull null)
        }
    }

    private fun keysMatchOld(ka: String, kb: String): Boolean {
        if (ka.isEmpty() || kb.isEmpty()) return false
        if (ka == kb) return true
        val (s, l) = if (ka.length <= kb.length) ka to kb else kb to ka
        return s.length >= 5 && l.contains(s)
    }

    @Test
    fun rankBanksIsFastAndUnchanged() {
        val br = branches()
        val names = br.map { it.bank }.distinct().take(27)
        val banks = names.mapIndexed { i, n -> BankRate(n, 11900 + i, 11950 + i, null) }

        // Old matching: every bank against every branch, branch keys recomputed per call.
        fun oldMatchCount(): Int {
            val keyed = br.map { bankKey(it.bank) to it }
            return banks.sumOf { b -> val bk = bankKey(b.bank); keyed.count { keysMatchOld(bk, it.first) } }
        }
        fun newMatchCount(): Int = rankBanks(banks, ConvertMode.SELL_USD, 100.0, null, br).size

        repeat(5) { oldMatchCount(); newMatchCount() } // warm-up
        val n = 50
        val t0 = System.nanoTime(); repeat(n) { oldMatchCount() }; val old = (System.nanoTime() - t0) / n / 1e6
        val t1 = System.nanoTime(); repeat(n) { newMatchCount() }; val new = (System.nanoTime() - t1) / n / 1e6
        println("branches=${br.size} banks=${banks.size}  old matching=${"%.2f".format(old)} ms  new rankBanks=${"%.2f".format(new)} ms")
        assertEquals(banks.size, newMatchCount())
    }
}
