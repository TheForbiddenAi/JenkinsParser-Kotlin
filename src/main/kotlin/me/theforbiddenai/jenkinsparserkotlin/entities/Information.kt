package me.theforbiddenai.jenkinsparserkotlin.entities

import org.jsoup.nodes.Element

abstract class Information {

    abstract val url: String
    abstract var name: String
    abstract var type: String
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
            while (nextElement.nodeName() == "dd") {
                infoStringBuilder.append("${nextElement.text()}\n")
                rawInfoBuilder.append("${nextElement.html()}\n")

                nextElement = nextElement.nextElementSibling() ?: break
            }

            extraInformation[it.text()] = infoStringBuilder.toString().trim()
            rawExtraInformation[it.text()] = rawInfoBuilder.toString().trim()
        }
    }

    /**
     * Gets the description from the given element and stores it in the `description` and `rawDescription` variables
     *
     * @param element The element to retrieve the description from
     */
    protected fun retrieveDescription(element: Element) {
        val descElementList = element.select("div.block")
        with(descElementList) {
            when (size) {
                0 -> "N/A"
                1 -> {
                    val descElement = descElementList[0]

                    description = descElement.text()
                    rawDescription = descElement.html()
                }
                else -> {
                    val normalStrBuilder = java.lang.StringBuilder()
                    val rawStrBuilder = java.lang.StringBuilder()

                    descElementList.forEach {
                        normalStrBuilder.append(it.html() + "<br>")
                        rawStrBuilder.append(it.text() + "\n")
                    }

                    description = normalStrBuilder.toString().trim()
                    rawDescription = rawStrBuilder.toString().trim()
                }
            }
        }
    }


    /**
     * An extension function to List<String> to allow for case insensitive matching on contains
     *
     * @param element The element in the list being searched for
     * @return True or false depending on whether the element is in the list, case insensitive
     */
    fun List<String>.containsIgnoreCase(element: String): Boolean {
        return this.stream().anyMatch { it.equals(element, true) }
    }

}