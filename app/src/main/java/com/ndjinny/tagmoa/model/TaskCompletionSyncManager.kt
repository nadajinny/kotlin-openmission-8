package com.ndjinny.tagmoa.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import com.google.firebase.database.DatabaseReference

/**
 * Persists pending task-completion updates so they survive process death/offline periods.
 * Firebase Realtime Database marks a write as "successful" as soon as it is queued locally,
 * so killing the app immediately could drop that queued write unless we reschedule it.
 */
object TaskCompletionSyncManager {

    private const val PREFS_NAME = "task_completion_sync"
    private const val KEY_PREFIX = "pending_tasks_"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun enqueue(userId: String, taskId: String, payload: CompletionPayload) {
        if (!::prefs.isInitialized) return
        val pending = loadPending(userId).toMutableMap()
        pending[taskId] = payload
        savePending(userId, pending)
    }

    fun markSynced(userId: String, taskId: String) {
        if (!::prefs.isInitialized) return
        val pending = loadPending(userId).toMutableMap()
        if (pending.remove(taskId) != null) {
            savePending(userId, pending)
        }
    }

    fun flushPending(userId: String, tasksRef: DatabaseReference) {
        if (!::prefs.isInitialized) return
        val pending = loadPending(userId)
        if (pending.isEmpty()) return
        pending.forEach { (taskId, payload) ->
            val updates = payload.toUpdateMap()
            tasksRef.child(taskId).updateChildren(updates)
                .addOnSuccessListener {
                    markSynced(userId, taskId)
                }
                .addOnFailureListener {
                    // If it still fails (e.g., permission denied) drop it to avoid endless retries.
                    markSynced(userId, taskId)
                }
        }
    }

    fun getPendingForUser(userId: String): Map<String, CompletionPayload> {
        return loadPending(userId)
    }

    private fun loadPending(userId: String): Map<String, CompletionPayload> {
        if (!::prefs.isInitialized) return emptyMap()
        val raw = prefs.getString(keyFor(userId), null) ?: return emptyMap()
        val result = mutableMapOf<String, CompletionPayload>()
        return try {
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val taskId = keys.next()
                val payload = CompletionPayload.fromJson(json.optJSONObject(taskId))
                if (payload != null) {
                    result[taskId] = payload
                }
            }
            result
        } catch (_: Exception) {
            prefs.edit().remove(keyFor(userId)).apply()
            emptyMap()
        }
    }

    private fun savePending(userId: String, data: Map<String, CompletionPayload>) {
        if (!::prefs.isInitialized) return
        val editor = prefs.edit()
        if (data.isEmpty()) {
            editor.remove(keyFor(userId)).apply()
            return
        }
        val json = JSONObject()
        data.forEach { (taskId, payload) ->
            json.put(taskId, payload.toJson())
        }
        editor.putString(keyFor(userId), json.toString()).apply()
    }

    private fun keyFor(userId: String): String = "$KEY_PREFIX$userId"

    data class CompletionPayload(
        val isCompleted: Boolean,
        val completedAt: Long?,
        val dueDate: Long?,
        val updatesDueDate: Boolean
    ) {
        fun toUpdateMap(): MutableMap<String, Any?> {
            val updates = mutableMapOf<String, Any?>(
                "isCompleted" to isCompleted,
                "completedAt" to completedAt
            )
            if (updatesDueDate) {
                updates["dueDate"] = dueDate
            }
            return updates
        }

        fun toJson(): JSONObject = JSONObject().apply {
            put("isCompleted", isCompleted)
            if (completedAt != null) {
                put("completedAt", completedAt)
            } else {
                put("completedAt", JSONObject.NULL)
            }
            if (dueDate != null) {
                put("dueDate", dueDate)
            } else {
                put("dueDate", JSONObject.NULL)
            }
            put("updatesDueDate", updatesDueDate)
        }

        companion object {
            fun fromJson(json: JSONObject?): CompletionPayload? {
                json ?: return null
                return CompletionPayload(
                    isCompleted = json.optBoolean("isCompleted", false),
                    completedAt = if (json.isNull("completedAt")) null else json.optLong("completedAt"),
                    dueDate = if (json.isNull("dueDate")) null else json.optLong("dueDate"),
                    updatesDueDate = json.optBoolean("updatesDueDate", false)
                )
            }
        }
    }
}
