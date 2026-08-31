package com.tatl.fastnote

import com.tatl.fastnote.billing.TrialManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialManagerTest {

    @Test
    fun testTrialDaysCalculation() {
        // TRIAL_DAYS must be 30
        assertEquals(30L, TrialManager.TRIAL_DAYS)
    }
}
