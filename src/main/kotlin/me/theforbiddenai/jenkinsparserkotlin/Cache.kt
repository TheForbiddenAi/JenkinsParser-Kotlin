package me.theforbiddenai.jenkinsparserkotlin

import me.theforbiddenai.jenkinsparserkotlin.entities.Information

internal class Cache<T : Information> {

    private val cacheMap = mutableMapOf<String, T>()

    fun retrieveInformation(url: String) : T? {
        return cacheMap[url.toLowerCase()]
    }

    fun addInformation(url: String, info: T) {
        if(containsUrl(url)) return
        cacheMap[url.toLowerCase()] = info
    }

    fun removeInformation(url: String) {
        cacheMap.remove(url.toLowerCase())
    }

    fun containsUrl(url: String) : Boolean{
        return cacheMap.containsKey(url.toLowerCase())
    }

    fun clear() = cacheMap.clear()

}