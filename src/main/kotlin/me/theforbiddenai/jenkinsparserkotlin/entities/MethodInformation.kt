package me.theforbiddenai.jenkinsparserkotlin.entities

import me.theforbiddenai.jenkinsparserkotlin.Cache
import org.jsoup.nodes.Element

private val methodCache = Cache()

data class MethodInformation internal constructor(
    val classInfo: ClassInformation,
    val methodElement: Element,
    override val url: String,
    override val name: String
) : Information() {

    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    init {
        val foundInformation = methodCache.getInformation(url)

        if (foundInformation != null) {
            val foundMethod = foundInformation as MethodInformation
            retrieveDataFromCache(foundMethod)
        } else {
            retrieveDataFromElement()
            methodCache.addInformation(url, this)
        }
    }

    private fun retrieveDataFromElement() {
        val descriptionElement = methodElement.selectFirst("div.block")

        description = descriptionElement?.text() ?: "N/A"
        rawDescription = descriptionElement?.html() ?: "N/A"

        retrieveExtraInfo(methodElement)
    }

    private fun retrieveDataFromCache(methodInfo: MethodInformation) {
        description = methodInfo.description
        rawDescription = methodInfo.rawDescription
        extraInformation = methodInfo.extraInformation
        rawExtraInformation = methodInfo.rawExtraInformation
    }
}