package me.theforbiddenai.jenkinsparserkotlin.entities

import org.jsoup.nodes.Element

data class MethodInformation internal constructor(
    val classInfo: ClassInformation,
    val methodElement: Element,
    override val url: String,
    override var name: String
) : Information() {

    override var type: String = "Method"
    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    init {
        retrieveDescription(methodElement)
        retrieveExtraInfo(methodElement)
    }


}