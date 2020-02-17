package me.theforbiddenai.jenkinsparserkotlin.entities

import me.theforbiddenai.jenkinsparserkotlin.Cache
import org.jsoup.nodes.Element

private val fieldCache = Cache()

data class FieldInformation internal constructor(
    val classInfo: ClassInformation,
    val fieldElement: Element,
    override val url: String,
    override val name: String
) : Information() {

    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    init {
        val foundInformation = fieldCache.getInformation(url)

        if (foundInformation != null) {
            val foundField = foundInformation as FieldInformation
            retrieveDataFromCache(foundField)
        } else {
            retrieveDataFromElement()
            fieldCache.addInformation(url, this)
        }
    }

    private fun retrieveDataFromElement() {
        val descriptionElement = fieldElement.selectFirst("div.block")

        description = descriptionElement?.text() ?: "N/A"
        rawDescription = descriptionElement?.html() ?: "N/A"

        retrieveExtraInfo(fieldElement)
    }

    private fun retrieveDataFromCache(fieldInfo: FieldInformation) {
        description = fieldInfo.description
        rawDescription = fieldInfo.rawDescription
        extraInformation = fieldInfo.extraInformation
        rawExtraInformation = fieldInfo.rawExtraInformation
    }
}