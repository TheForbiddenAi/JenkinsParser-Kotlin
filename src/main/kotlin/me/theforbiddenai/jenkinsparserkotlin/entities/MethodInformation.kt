package me.theforbiddenai.jenkinsparserkotlin.entities

import org.jsoup.nodes.Element

data class MethodInformation internal constructor(
    val classInfo: ClassInformation,
    val methodElement: Element,
    override val url: String,
    override val name: String
) : Information() {

    override lateinit var type: String
    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    init {
        val descriptionElement = methodElement.selectFirst("div.block")

        type = "Method"
        description = descriptionElement?.text() ?: "N/A"
        rawDescription = descriptionElement?.html() ?: "N/A"

        retrieveExtraInfo(methodElement)
    }

}