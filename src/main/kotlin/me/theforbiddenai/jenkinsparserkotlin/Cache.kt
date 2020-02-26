package me.theforbiddenai.jenkinsparserkotlin

import me.theforbiddenai.jenkinsparserkotlin.entities.Information

internal class Cache {

    private val cacheMap = mutableMapOf<String, Information>()

    fun getInformation(key: String) : Information? {
        return cacheMap[key]
    }

    fun addInformation(key: String, info: Information) {
        cacheMap[key] = info
    }

    fun removeInformation(key: String) {
        cacheMap.remove(key)
    }

    fun containsInformation(key: String) : Boolean{
        return cacheMap.containsKey(key)
    }

    fun clear() = cacheMap.clear()

}