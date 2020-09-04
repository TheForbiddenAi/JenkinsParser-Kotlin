package me.theforbiddenai.jenkinsparserkotlin.entities

import org.jsoup.nodes.Element

data class FieldInformation internal constructor(
    val classInfo: ClassInformation,
    val fieldElement: Element,
    override val url: String,
    override val name: String
) : Information() {

    override var type: String = "Field"
    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    init {
        retrieveDescription(fieldElement)
        retrieveExtraInfo(fieldElement)
    }

}