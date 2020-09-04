package me.theforbiddenai.jenkinsparserkotlin.entities

import org.jsoup.nodes.Element

data class EnumInformation internal constructor(
    val classInfo: ClassInformation,
    val enumElement: Element,
    override val url: String,
    override val name: String
) : Information() {

    override var type: String = "Enum"
    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    init {
        retrieveDescription(enumElement)
        retrieveExtraInfo(enumElement)
    }

}