package me.theforbiddenai.jenkinsparserkotlin.entities

import org.jsoup.nodes.Element

abstract class Information  {

    abstract val url: String
    abstract val name: String
    abstract var description: String
    abstract var rawDescription: String
    abstract var extraInformation: MutableMap<String, String>
    abstract var rawExtraInformation: MutableMap<String, String>

    /**
     * Puts the extra information from a given element into the extra information maps
     * (Extra information pertains to things like parameters)
     *
     * @param element The element the extra information is pulled from
     */
    protected fun retrieveExtraInfo(element: Element) {
        val dlElement = element.select("dl") ?: return

        dlElement.select("dt").forEach {
            var nextElement = it.nextElementSibling() ?: return

            val infoStringBuilder = StringBuilder()
            val rawInfoBuilder = StringBuilder()
            while(nextElement.nodeName() == "dd") {
                infoStringBuilder.append("${nextElement.text()}\n")
                rawInfoBuilder.append("${nextElement.html()}\n")

                nextElement = nextElement.nextElementSibling() ?: break
            }

            extraInformation[it.text()] = infoStringBuilder.toString().trim()
            rawExtraInformation[it.text()] = rawInfoBuilder.toString().trim()
        }
    }

}