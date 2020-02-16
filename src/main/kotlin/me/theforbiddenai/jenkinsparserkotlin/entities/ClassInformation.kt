package me.theforbiddenai.jenkinsparserkotlin.entities

import me.theforbiddenai.jenkinsparserkotlin.Cache
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

private val classCache = Cache()
private val hiddenUnicodeRegex = "\\p{C}".toRegex()
private val annotationRegex = "[@]\\w+\\s+".toRegex()
private val methodNameRegex = "\\w+\\s?\\(.*\\)".toRegex()
private val whitespaceRegex = "\\s+".toRegex()

data class ClassInformation internal constructor(
    override var url: String,
    override var name: String
) : Information() {

    private val classDocument = Jsoup.connect(url).get()

    private val methodMap: MutableMap<String, Element> = mutableMapOf()
    private val enumMap: MutableMap<String, Element> = mutableMapOf()
    private val fieldMap: MutableMap<String, Element> = mutableMapOf()

    val methodList: MutableList<String> = mutableListOf()
    val enumList: MutableList<String> = mutableListOf()
    val fieldList: MutableList<String> = mutableListOf()

    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    init {
        val foundInformation = classCache.getInformation(name)

        if (foundInformation != null) {
            val foundClass = foundInformation as ClassInformation
            retrieveDataFromCache(foundClass)
        } else {
            retrieveDataFromDocument()
            classCache.addInformation(this)
        }
    }

    /**
     * Retrieves all methods with a specified name
     *
     * @param query The name of the methods being searched for
     * @return Returns a list of all found methods
     */
    fun retrieveMethods(query: String): List<MethodInformation> {
        val foundMethodList = mutableListOf<MethodInformation>()

        retrieveInfo(methodMap, query).forEach { (methodName, methodUrl) ->
            val fieldElement = methodMap[methodName] ?: return@forEach
            foundMethodList.add(MethodInformation(this, fieldElement, methodUrl, methodName))
        }

        return foundMethodList
    }

    /**
     * Retrieves the enum with a specified name
     *
     * @param query The name of the enum being searched for
     * @return The found enum
     */
    fun retrieveEnum(query: String): EnumInformation {
        retrieveInfo(enumMap, query).forEach { (enumName, enumUrl) ->
            val fieldElement = enumMap[enumName] ?: return@forEach
            return EnumInformation(this, fieldElement, enumUrl, enumName)
        }

        throw Exception("Could not find an enum with the name of $query in the class $name")
    }

    /**
     * Retrieves the field with a specified name
     *
     * @param query The name of the fields being searched for
     * @return Returns the found field
     */
    fun retrieveField(query: String): FieldInformation {
        retrieveInfo(fieldMap, query).forEach { (fieldName, fieldUrl) ->
            val fieldElement = fieldMap[fieldName] ?: return@forEach
            return FieldInformation(this, fieldElement, fieldUrl, fieldName)
        }

        throw Exception("Could not find a field with the name of $query in the class $name")
    }

    /**
     * Retrieves the name and url from a query and a map
     *
     * @param infoMap The map being searched in
     * @param query The information being searched for
     * @return A map containing the name and url of the found information
     */
    private fun retrieveInfo(infoMap: MutableMap<String, Element>, query: String): MutableMap<String, String> {
        val foundList = mutableMapOf<String, String>()

        infoMap.filter { (infoName, _) ->
            val modifiedName = infoName.substringBefore("(").trim()
            return@filter modifiedName.equals(query, true)
        }
            .forEach { (infoName, infoElement) ->
                val anchorElement = infoElement.parent().previousElementSibling() ?: return@forEach

                val anchorAttr = anchorElement.attr("id") ?: anchorElement.attr("name")
                val infoUrl = "$url#${anchorAttr}"

                foundList[infoName] = infoUrl
            }

        return foundList
    }

    /**
     * Gets the block list by getting an anchor's parent and turns the information into a map
     *
     * @param listId The anchor's id
     * @return A map of the name and element of the found information
     */
    private fun getItemList(listId: String): MutableMap<String, Element> {
        val returnMap = mutableMapOf<String, Element>()

        val anchor = classDocument.selectFirst("a[id=$listId]")
            ?: classDocument.selectFirst("a[name=$listId]")
            ?: return returnMap

        val blockList = anchor.parent()

        blockList.select("li.blockList").forEach {

            val signature = it.selectFirst("pre").text()

            var itemName = signature.replace(hiddenUnicodeRegex, " ")

            itemName = if (listId.equals("method.detail", true)) {
                itemName.replace(annotationRegex, "")
                    .replace(whitespaceRegex, " ").trim()

                methodNameRegex.find(itemName)?.value
                    ?: throw Exception("Failed to match method signature: $signature");
            } else {
                itemName.substringAfterLast(" ")
            }


            returnMap[itemName] = it
        }

        return returnMap
    }

    private fun retrieveDataFromDocument() {
        name = classDocument.selectFirst("h2").text() ?: "N/A"
        name = name.replaceBefore(" ", "").trim()

        val descriptionElement = classDocument.selectFirst("div.description")
            .selectFirst("div.block")

        description = descriptionElement?.text() ?: "N/A"
        rawDescription = descriptionElement?.html() ?: "N/A"

        methodMap.putAll(getItemList("method.detail"))
        methodList.addAll(methodMap.keys)

        enumMap.putAll(getItemList("enum.constant.detail"))
        enumList.addAll(enumMap.keys)

        fieldMap.putAll(getItemList("field.detail"))
        fieldList.addAll(fieldMap.keys)
    }

    private fun retrieveDataFromCache(classInfo: ClassInformation) {
        name = classInfo.name
        description = classInfo.description
        rawDescription = classInfo.rawDescription
        extraInformation = classInfo.extraInformation
        rawExtraInformation = classInfo.rawExtraInformation

        methodMap.putAll(classInfo.methodMap)
        methodList.addAll(classInfo.methodList)

        enumMap.putAll(classInfo.enumMap)
        enumList.addAll(classInfo.enumList)

        fieldMap.putAll(classInfo.fieldMap)
        fieldList.addAll(classInfo.fieldList)
    }

}