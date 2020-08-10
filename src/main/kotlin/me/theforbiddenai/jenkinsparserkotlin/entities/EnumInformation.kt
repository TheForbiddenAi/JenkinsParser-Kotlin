package me.theforbiddenai.jenkinsparserkotlin.entities

import org.jsoup.nodes.Element

data class EnumInformation internal constructor(
    val classInfo: ClassInformation,
    val enumElement: Element,
    override val url: String,
    override val name: String
) : Information() {

    override lateinit var type: String
    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    init {
        val descriptionElement = enumElement.selectFirst("div.block")

        type = "Enum"
        description = descriptionElement?.text() ?: "N/A"
        rawDescription = descriptionElement?.html() ?: "N/A"

        retrieveExtraInfo(enumElement)
    }

}