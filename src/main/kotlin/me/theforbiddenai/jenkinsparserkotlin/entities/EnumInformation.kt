package me.theforbiddenai.jenkinsparserkotlin.entities

import me.theforbiddenai.jenkinsparserkotlin.Cache
import org.jsoup.nodes.Element

private val enumCache = Cache()

data class EnumInformation internal constructor(
    val classInfo: ClassInformation,
    val enumElement: Element,
    override val url: String,
    override val name: String
) : Information() {

    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    init {
        val foundInformation = enumCache.getInformation(name)

        if (foundInformation != null) {
            val foundEnum = foundInformation as EnumInformation
            retrieveDataFromCache(foundEnum)
        } else {
            retrieveDataFromElement()
            enumCache.addInformation(this)
        }
    }

    private fun retrieveDataFromElement() {
        val descriptionElement = enumElement.selectFirst("div.block")

        description = descriptionElement?.text() ?: "N/A"
        rawDescription = descriptionElement?.html() ?: "N/A"

        retrieveExtraInfo(enumElement)
    }

    private fun retrieveDataFromCache(enu: EnumInformation) {
        description = enu.description
        rawDescription = enu.rawDescription
        extraInformation = enu.extraInformation
        rawExtraInformation = enu.rawExtraInformation
    }
}