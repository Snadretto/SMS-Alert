/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.UUID

class AppStore(context: Context, name: String = "alerts.db", private val changed: () -> Unit = {}) : SQLiteOpenHelper(context, name, null, 2) {
    private val appContext = context.applicationContext
    companion object { private val lock = Any() }

    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE settings (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE rules (id TEXT PRIMARY KEY, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, received_at INTEGER NOT NULL, status TEXT NOT NULL, resolved_at INTEGER, payload TEXT NOT NULL)")
        db.execSQL("CREATE INDEX event_status ON events(status, received_at)")
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        createDedupe(db)
        db.insertOrThrow("settings", null, ContentValues().apply { put("id", 1); put("payload", JSONObject(AlarmSettings().toMap()).toString()) })
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) createDedupe(db)
        else throw IllegalStateException("Nicht unterstützte Datenbankmigration")
    }

    private fun createDedupe(db: SQLiteDatabase) = db.execSQL("CREATE TABLE IF NOT EXISTS seen (fingerprint TEXT PRIMARY KEY, expires_at INTEGER NOT NULL)")

    private fun <T> transaction(block: (SQLiteDatabase) -> T): T = synchronized(lock) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = block(db)
            db.setTransactionSuccessful()
            result
        } finally { db.endTransaction(); changed() }
    }

    fun settings(): AlarmSettings = synchronized(lock) { readSettings(readableDatabase) }

    private fun readSettings(db: SQLiteDatabase): AlarmSettings = db.rawQuery("SELECT payload FROM settings WHERE id=1", null).use {
        if (!it.moveToFirst()) throw AppFailure("storage", "Einstellungen konnten nicht gelesen werden.")
        settingsFromJson(JSONObject(it.getString(0)))
    }

    private fun settingsFromJson(j: JSONObject): AlarmSettings = AlarmSettings(
        enabled = j.optBoolean("enabled", false), country = j.optString("country", "CH"),
        volume = j.optInt("volume", 80), vibration = j.optBoolean("vibration", true),
        tone = j.optString("tone", "beacon"), raiseSystemVolume = j.optBoolean("raiseSystemVolume", false),
        retentionDays = j.optInt("retentionDays", 30), historyLimit = j.optInt("historyLimit", 200),
        deviceTone = j.optJSONObject("deviceTone")?.let { DeviceTone(it.getString("id"), it.getString("title"), it.getString("uri")) },
    ).also { it.validate() }

    private fun writeSettings(db: SQLiteDatabase, settings: AlarmSettings) {
        settings.validate()
        db.update("settings", ContentValues().apply { put("payload", JSONObject(settings.toMap()).toString()) }, "id=1", null)
    }

    fun updateSettings(args: Map<*, *>) = transaction { db ->
        val current = readSettings(db)
        val next = current.copy(
            country = args.string("country", current.country).uppercase(Locale.ROOT),
            volume = args.integer("volume", current.volume), vibration = args.boolean("vibration", current.vibration),
            tone = args.string("tone", current.tone), raiseSystemVolume = args.boolean("raiseSystemVolume", current.raiseSystemVolume),
            retentionDays = args.integer("retentionDays", current.retentionDays), historyLimit = args.integer("historyLimit", current.historyLimit),
            deviceTone = if (!args.containsKey("deviceTone")) current.deviceTone else args["deviceTone"]?.let {
                val selected = it as? Map<*, *> ?: throw AppFailure("invalid_tone", "Ungültige Klingeltonauswahl.")
                DeviceTone(selected.string("id", ""), selected.string("title", ""), selected.string("uri", ""))
            },
        )
        next.validate()
        if (next.tone == "device") ToneAssets.requireAvailable(appContext, next.deviceTone!!)
        writeSettings(db, next)
        prune(db, next)
    }

    fun setEnabled(enabled: Boolean) = transaction { db ->
        if (enabled && readRules(db).none { it.enabled }) throw AppFailure("no_rules", "Zuerst mindestens eine aktive Regel anlegen.")
        writeSettings(db, readSettings(db).copy(enabled = enabled))
        if (!enabled) {
            db.update("events", ContentValues().apply { put("status", "PAUSED"); put("resolved_at", System.currentTimeMillis()) }, "status='PENDING'", null)
            putMeta(db, "lastOutcome", "paused")
        }
    }

    fun rules(): List<AlarmRule> = synchronized(lock) { readRules(readableDatabase) }

    private fun readRules(db: SQLiteDatabase): List<AlarmRule> = db.rawQuery("SELECT payload FROM rules ORDER BY rowid", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(ruleFromJson(JSONObject(cursor.getString(0)))) }
    }

    private fun ruleFromJson(j: JSONObject): AlarmRule = AlarmRule(
        id = j.getString("id"), name = j.getString("name"), sender = j.optString("sender", ""), text = j.optString("text", ""),
        logic = j.optString("logic", "AND"), enabled = j.optBoolean("enabled", true), caseSensitive = j.optBoolean("caseSensitive", false),
        simSlot = if (j.isNull("simSlot")) null else j.getInt("simSlot"),
    ).also { it.validate() }

    fun ruleFromArguments(args: Map<*, *>): AlarmRule = AlarmRule(
        id = args.string("id", "").ifBlank { UUID.randomUUID().toString() },
        name = args.string("name", "").trim(), sender = args.string("sender", "").trim(), text = args.string("text", "").trim(),
        logic = args.string("logic", "AND"), enabled = args.boolean("enabled", true), caseSensitive = args.boolean("caseSensitive", false),
        simSlot = args["simSlot"]?.let { (it as? Number)?.toInt() ?: throw AppFailure("invalid_rule", "Ungültiger SIM-Filter.") },
    ).also { it.validate(); if (it.id.length > 80) throw AppFailure("invalid_rule", "Ungültige Regel-ID.") }

    fun saveRule(rule: AlarmRule) = transaction { db ->
        rule.validate()
        db.insertWithOnConflict("rules", null, ContentValues().apply { put("id", rule.id); put("payload", JSONObject(rule.toMap()).toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteRule(id: String) = transaction { db -> db.delete("rules", "id=?", arrayOf(id)) }

    fun receive(sms: SmsInput): String? = transaction { db ->
        val settings = readSettings(db)
        if (!settings.enabled) return@transaction null
        val matching = readRules(db).filter { it.matches(sms.sender, sms.body, sms.simSlot, settings.country) }
        if (matching.isEmpty()) return@transaction null
        val fingerprint = TransportIdentity.fingerprint(secret(db), sms)
        val duplicate = db.rawQuery("SELECT 1 FROM seen WHERE fingerprint=? AND expires_at>=?", arrayOf(fingerprint, sms.receivedAt.toString())).use { it.moveToFirst() }
        if (duplicate) return@transaction null
        db.delete("seen", "expires_at<?", arrayOf(sms.receivedAt.toString()))
        db.insertWithOnConflict("seen", null, ContentValues().apply { put("fingerprint", fingerprint); put("expires_at", sms.receivedAt + 120_000) }, SQLiteDatabase.CONFLICT_REPLACE)
        val id = UUID.randomUUID().toString()
        insertEvent(db, linkedMapOf(
            "id" to id, "receivedAt" to sms.receivedAt, "receivedElapsedMs" to sms.elapsedMs, "bootCount" to sms.bootCount,
            "sentAt" to sms.sentAt, "source" to "SMS", "ruleIds" to matching.map { it.id }, "ruleNames" to matching.map { it.name },
            "senderLabel" to SenderIdentity.masked(sms.sender), "simSlot" to sms.simSlot,
        ))
        putMeta(db, "lastOutcome", "alarm_queued")
        prune(db, settings)
        id
    }

    fun addTest(now: Long, elapsed: Long, bootCount: Int): String = transaction { db ->
        if (!readSettings(db).enabled) throw AppFailure("paused", "Die App ist pausiert. Zum Testen bitte ausdrücklich aktivieren.")
        val id = UUID.randomUUID().toString()
        insertEvent(db, linkedMapOf(
            "id" to id, "receivedAt" to now, "receivedElapsedMs" to elapsed, "bootCount" to bootCount,
            "sentAt" to now, "source" to "TEST", "ruleIds" to emptyList<String>(), "ruleNames" to listOf("Lokaler Testalarm"),
            "senderLabel" to "Test auf diesem Gerät", "simSlot" to null,
        ))
        putMeta(db, "lastOutcome", "alarm_queued")
        id
    }

    private fun insertEvent(db: SQLiteDatabase, event: Map<String, Any?>) {
        db.insertOrThrow("events", null, ContentValues().apply {
            put("id", event["id"] as String); put("received_at", event["receivedAt"] as Long)
            put("status", "PENDING"); put("payload", JSONObject(event).toString())
        })
    }

    fun acknowledge(id: String): Boolean = transaction { db ->
        val count = db.update("events", ContentValues().apply { put("status", "ACKNOWLEDGED"); put("resolved_at", System.currentTimeMillis()) }, "id=? AND status='PENDING'", arrayOf(id))
        if (count > 0) putMeta(db, "lastOutcome", "acknowledged")
        prune(db, readSettings(db))
        count > 0
    }

    fun acknowledgeAll() = transaction { db ->
        db.update("events", ContentValues().apply { put("status", "ACKNOWLEDGED"); put("resolved_at", System.currentTimeMillis()) }, "status='PENDING'", null)
        putMeta(db, "lastOutcome", "acknowledged")
        prune(db, readSettings(db))
    }

    fun recordOutcome(id: String, outcome: String, latencyMs: Long? = null) = transaction { db ->
        val event = db.rawQuery("SELECT payload FROM events WHERE id=? AND status='PENDING'", arrayOf(id)).use {
            if (!it.moveToFirst()) return@transaction
            JSONObject(it.getString(0))
        }
        event.put("lastOutcome", outcome)
        if (outcome == "playback_started" && !event.has("startedAt")) event.put("startedAt", System.currentTimeMillis())
        if (latencyMs != null && !event.has("latencyMs")) event.put("latencyMs", latencyMs)
        db.update("events", ContentValues().apply { put("payload", event.toString()) }, "id=? AND status='PENDING'", arrayOf(id))
    }

    fun pending(): List<Map<String, Any?>> = synchronized(lock) { eventQuery(readableDatabase, "status='PENDING'", "rowid ASC", 500) }
    fun pendingCount(): Int = synchronized(lock) { readableDatabase.rawQuery("SELECT count(*) FROM events WHERE status='PENDING'", null).use { it.moveToFirst(); it.getInt(0) } }
    fun nextPending(): Map<String, Any?>? = synchronized(lock) { eventQuery(readableDatabase, "status='PENDING'", "rowid ASC", 1).firstOrNull() }
    fun history(): List<Map<String, Any?>> = synchronized(lock) { eventQuery(readableDatabase, "status!='PENDING'", "received_at DESC, rowid DESC", readSettings(readableDatabase).historyLimit) }

    private fun eventQuery(db: SQLiteDatabase, where: String, order: String, limit: Int): List<Map<String, Any?>> =
        db.rawQuery("SELECT payload,status,resolved_at FROM events WHERE $where ORDER BY $order LIMIT ?", arrayOf(limit.toString())).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val item = jsonMap(JSONObject(cursor.getString(0))).toMutableMap()
                    item["status"] = cursor.getString(1)
                    item["resolvedAt"] = if (cursor.isNull(2)) null else cursor.getLong(2)
                    add(item)
                }
            }
        }

    fun clearHistory() = transaction { db -> db.delete("events", "status!='PENDING'", null) }
    fun maintain() = transaction { db -> prune(db, readSettings(db)) }

    private fun prune(db: SQLiteDatabase, settings: AlarmSettings) {
        val cutoff = System.currentTimeMillis() - settings.retentionDays * 86_400_000L
        db.delete("events", "status!='PENDING' AND resolved_at<?", arrayOf(cutoff.toString()))
        db.execSQL("DELETE FROM events WHERE status!='PENDING' AND id NOT IN (SELECT id FROM events WHERE status!='PENDING' ORDER BY received_at DESC,rowid DESC LIMIT ?)", arrayOf(settings.historyLimit))
    }

    fun meta(key: String): String? = synchronized(lock) { getMeta(readableDatabase, key) }
    fun setMeta(key: String, value: String) = transaction { db -> putMeta(db, key, value) }
    fun removeMeta(key: String) = transaction { db -> db.delete("meta", "key=?", arrayOf(key)) }
    private fun getMeta(db: SQLiteDatabase, key: String): String? = db.rawQuery("SELECT value FROM meta WHERE key=?", arrayOf(key)).use { if (it.moveToFirst()) it.getString(0) else null }
    private fun putMeta(db: SQLiteDatabase, key: String, value: String) { db.insertWithOnConflict("meta", null, ContentValues().apply { put("key", key); put("value", value) }, SQLiteDatabase.CONFLICT_REPLACE) }

    private fun secret(db: SQLiteDatabase): ByteArray {
        val existing = getMeta(db, "dedupeSecret")
        if (existing != null) return Base64.getDecoder().decode(existing)
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        putMeta(db, "dedupeSecret", Base64.getEncoder().encodeToString(bytes))
        return bytes
    }
}

fun jsonMap(json: JSONObject): Map<String, Any?> = json.keys().asSequence().associateWith { jsonValue(json.get(it)) }
private fun jsonValue(value: Any?): Any? = when (value) {
    JSONObject.NULL -> null
    is JSONObject -> jsonMap(value)
    is JSONArray -> (0 until value.length()).map { jsonValue(value.get(it)) }
    else -> value
}

fun Map<*, *>.string(key: String, default: String): String = if (!containsKey(key)) default else this[key] as? String ?: throw AppFailure("invalid_argument", "Ungültiger Textwert: $key")
fun Map<*, *>.boolean(key: String, default: Boolean): Boolean = if (!containsKey(key)) default else this[key] as? Boolean ?: throw AppFailure("invalid_argument", "Ungültiger Schalter: $key")
fun Map<*, *>.integer(key: String, default: Int): Int {
    if (!containsKey(key)) return default
    val value = this[key] as? Number ?: throw AppFailure("invalid_argument", "Ungültiger Zahlenwert: $key")
    if (value.toDouble() != value.toInt().toDouble()) throw AppFailure("invalid_argument", "Ganzzahl erforderlich: $key")
    return value.toInt()
}
