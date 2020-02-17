package me.theforbiddenai.jenkinsparserkotlin

import me.theforbiddenai.jenkinsparserkotlin.entities.Information

internal class Cache {

    private val cacheMap = mutableMapOf<String, Information>()

    fun getInformation(key: String) : Information? {
        return cacheMap[key.toLowerCase()]
    }

    fun addInformation(key: String, info: Information) {
        cacheMap[key] = info
    }

    fun removeInformation(key: String) {
        cacheMap.remove(key.toLowerCase())
    }

    fun clear() = cacheMap.clear()

}