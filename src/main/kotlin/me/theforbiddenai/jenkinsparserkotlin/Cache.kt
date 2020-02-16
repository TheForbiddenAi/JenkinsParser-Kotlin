package me.theforbiddenai.jenkinsparserkotlin

import me.theforbiddenai.jenkinsparserkotlin.entities.Information

internal class Cache {

    private val cacheMap = mutableMapOf<String, Information>()

    fun getInformation(name: String) : Information? {
        return cacheMap[name.toLowerCase()]
    }

    fun addInformation(info: Information) {
        val name = info.name.toLowerCase()
        cacheMap[name] = info
    }

    fun removeInformation(name: String) {
        cacheMap.remove(name.toLowerCase())
    }

    fun clear() = cacheMap.clear()

}