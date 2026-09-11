package com.example.data.bible.repository

import com.example.data.bible.local.BibleDao
import com.example.data.bible.local.ReadingPlanProgressEntity
import com.example.data.bible.model.PredefinedReadingPlans
import com.example.data.bible.model.ReadingPlanInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ReadingPlanRepository(private val bibleDao: BibleDao) {

    fun getPlanProgress(planId: String): Flow<List<ReadingPlanProgressEntity>> {
        return bibleDao.getPlanProgress(planId)
    }

    suspend fun markDayCompleted(planId: String, dayNumber: Int, completed: Boolean) = withContext(Dispatchers.IO) {
        bibleDao.setPlanDayCompleted(
            ReadingPlanProgressEntity(
                planId = planId,
                dayNumber = dayNumber,
                isCompleted = completed,
                completedTimestamp = if (completed) System.currentTimeMillis() else 0L
            )
        )
    }

    suspend fun resetPlan(planId: String) = withContext(Dispatchers.IO) {
        bibleDao.resetPlan(planId)
    }

    suspend fun syncPlanToCurrentDate(planId: String, targetDayNumber: Int) = withContext(Dispatchers.IO) {
        for (day in 1 until targetDayNumber) {
            bibleDao.setPlanDayCompleted(
                ReadingPlanProgressEntity(
                    planId = planId,
                    dayNumber = day,
                    isCompleted = true,
                    completedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun getAllPlans(): List<ReadingPlanInfo> = PredefinedReadingPlans.allPlans

    fun getPlan(planId: String): ReadingPlanInfo? = PredefinedReadingPlans.getPlanById(planId)
}
