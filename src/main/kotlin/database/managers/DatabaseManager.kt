package org.srino.database.managers

import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.DeleteOneModel
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.Document
import org.srino.database
import org.srino.debug.Debug
import org.srino.every
import org.srino.managers.gson
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

abstract class DatabaseManager<Key: Any, Value: Any>(val name: String, application: Application, val keyClazz: Class<Key>, val valueClazz: Class<Value>) {

    open fun onDataLoad() {}

    private val data: MutableMap<Key, Value> = mutableMapOf()
    private val changes: MutableMap<Key, Value> = mutableMapOf()
    private val removed: MutableSet<Key> = mutableSetOf()

    private val collection = database.database.getCollection(name)

    operator fun set(key: Key, value: Value) {
        Debug.send("Setting ${key.toJson()} to ${value.toJson()}")
        data[key] = value
        changes[key] = value
        removed.remove(key)
    }

    operator fun get(key: Key): Value? {
        return data[key]
    }

    operator fun invoke(): Map<Key, Value> {
        return data.toMap()
    }

    fun delete(key: Key) {
        Debug.send("Deleting ${key.toJson()}")
        data.remove(key)
        changes.remove(key)
        removed.add(key)
    }

    init {
        Debug.send("Initializing database manager $name")
        application.launch {
            load()
        }
        startSaving(application)
    }

    fun startSaving(application: Application) {
        application.launch {
            every(5.minutes) {
                save()
            }
        }
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        collection.find().forEach { doc ->
            val keyJson = doc.getString("_id")
            val value = doc.get("value", Document::class.java)
            val valueJson = value.toJson()
            val obj = gson.fromJson(valueJson, valueClazz)
            val key = gson.fromJson(keyJson, keyClazz)
            data[key] = obj
        }
        Debug.send("Loaded ${data.size} entries for $name")
        onDataLoad()
    }

    suspend fun save() = withContext(Dispatchers.IO) {
        Debug.send("Saving data... for $name")
        removeKeys()
        updateChanges()
    }

    private fun removeKeys() {
        if (removed.isEmpty()) return
        val deleteModels = removed.map { key ->
            DeleteOneModel<Document>(Filters.eq("_id", key.toJson()))
        }
        collection.bulkWrite(deleteModels, BulkWriteOptions().ordered(false))
        removed.clear()
    }

    private fun updateChanges() {
        if (changes.isEmpty()) return

        val updateModels = changes.entries.map { (k, v) ->
            val keyJson = gson.toJson(k)
            val valueJson = gson.toJson(v)
            val objDocument = Document.parse(valueJson)

            ReplaceOneModel(
                Filters.eq("_id", keyJson),
                Document("_id", keyJson).append("value", objDocument),
                ReplaceOptions().upsert(true)
            )
        }
        changes.clear()
        collection.bulkWrite(updateModels, BulkWriteOptions().ordered(false))
    }

    suspend fun shutdown() {
        save()
    }

}

private fun Any.toJson(): String {
    return gson.toJson(this)
}

interface StringKey<T: Any> {
    fun manager(): DatabaseManager<String, T>
    fun id(): String

    fun register() {
        manager()[id()] = this as T
    }
    fun delete() {
        manager().delete(id())
    }
    fun update() = register()
}

interface UUIDKey<T: Any> {
    fun manager(): DatabaseManager<UUID, T>
    fun id(): UUID

    fun register() {
        manager()[id()] = this as T
    }
    fun delete() {
        manager().delete(id())
    }
    fun update() = register()
}