package me.theforbiddenai.jenkinsparserkotlin.entities

import me.theforbiddenai.jenkinsparserkotlin.Cache
import me.theforbiddenai.jenkinsparserkotlin.Jenkins
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

private val classCache = Cache()
private val hiddenUnicodeRegex = "\\p{C}".toRegex()
private val annotationRegex = "[@]\\w+\\s+".toRegex()
private val methodNameRegex = "\\w+\\s?\\(.*\\)".toRegex()
private val whitespaceRegex = "\\s+".toRegex()

data class ClassInformation internal constructor(
    val jenkins: Jenkins,
    override var url: String,
    override var name: String
) : Information() {

    private val classDocument = Jsoup.connect(url).get()

    private var nestedClassMap: MutableMap<String, String> = mutableMapOf()
    private var methodMap: MutableMap<String, Element> = mutableMapOf()
    private var enumMap: MutableMap<String, Element> = mutableMapOf()
    private var fieldMap: MutableMap<String, Element> = mutableMapOf()

    var nestedClassList: MutableList<String> = mutableListOf()
    var methodList: MutableList<String> = mutableListOf()
    var enumList: MutableList<String> = mutableListOf()
    var fieldList: MutableList<String> = mutableListOf()

    private var inheritedMethodMap: MutableMap<String, ClassInformation> = mutableMapOf()
    var inheritedMethodList: MutableList<String> = mutableListOf()

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
     * Retrieves a nested class with the specified name
     *
     * @param query The name of the nested class being searched for
     * @return Returns the found nested classes
     */
    fun retrieveNestedClass(query: String): ClassInformation {
        nestedClassMap.filter { (className, _) ->
            val modifiedClassName = className.replace("$name.", "").trim()
            val modifiedQuery = query.replace("$name.", "").trim()

            modifiedClassName.equals(modifiedQuery, true)
        }
            .forEach { (className, classUrl) -> return ClassInformation(jenkins, classUrl, className) }

        throw Exception("Could not find a nested class with the name $query in $name")
    }

    /**
     * Retrieves all methods defined in the class and all inherited methods with a specified name
     *
     * @param query The name of the methods being searched for
     * @return Returns a list of all found methods
     */
    fun searchAllMethods(query: String): List<MethodInformation> {
        val foundMethodList: MutableList<MethodInformation> = mutableListOf()

        foundMethodList.addAll(searchInheritedMethods(query))
        foundMethodList.addAll(searchMethods(query))

        return foundMethodList
    }

    /**
     * Retrieves all methods defined in the class with a specified name
     *
     * @param query The name of the methods being searched for
     * @return Returns a list of all found methods
     */
    fun searchMethods(query: String): List<MethodInformation> {
        val foundMethodList = mutableListOf<MethodInformation>()

        retrieveData(methodMap, query).forEach { (methodName, methodUrl) ->
            val fieldElement = methodMap[methodName] ?: return@forEach
            foundMethodList.add(MethodInformation(this, fieldElement, methodUrl, methodName))
        }

        return foundMethodList
    }

    /**
     * Retrieves all inherited methods in the class with a specified name
     *
     * @param query The name of the methods being searched for
     * @return Returns a list of all found methods
     */
    fun searchInheritedMethods(query: String): List<MethodInformation> {
        val foundMethodMap = mutableListOf<MethodInformation>()

        inheritedMethodMap.filter { (methodName, _) -> methodName.equals(query, true) }
            .forEach { (methodName, classInformation) ->
                val retrieveMethods = classInformation.searchMethods(methodName)
                foundMethodMap.addAll(retrieveMethods)
            }

        return foundMethodMap
    }

    /**
     * Retrieves the enum with a specified name
     *
     * @param query The name of the enum being searched for
     * @return Returns the found enum
     */
    fun retrieveEnum(query: String): EnumInformation {
        retrieveData(enumMap, query).forEach { (enumName, enumUrl) ->
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
        retrieveData(fieldMap, query).forEach { (fieldName, fieldUrl) ->
            val fieldElement = fieldMap[fieldName] ?: return@forEach
            return FieldInformation(this, fieldElement, fieldUrl, fieldName)
        }

        throw Exception("Could not find a field with the name of $query in the class $name")
    }

    /**
     * Retrieves the name and url from a query
     *
     * @param infoMap The map being searched in
     * @param query The information being searched for
     * @return A map containing the name and url of the found data
     */
    private fun retrieveData(infoMap: MutableMap<String, Element>, query: String): MutableMap<String, String> {
        val foundDataMap = mutableMapOf<String, String>()

        infoMap.filter { (infoName, _) ->
            val modifiedName = infoName.substringBefore("(").trim()
            return@filter modifiedName.equals(query, true)
        }
            .forEach { (infoName, infoElement) ->
                val anchorElement = infoElement.parent().previousElementSibling() ?: return@forEach

                val anchorAttr = anchorElement.attr("id") ?: anchorElement.attr("name")
                val infoUrl = "$url#${anchorAttr}"

                foundDataMap[infoName] = infoUrl
            }

        return foundDataMap
    }

    /**
     * Retrieves the name and url of items from a table
     *
     * @param tableId The id of the table the data is coming from
     * @return A map containing the name and url of the found data
     */
    private fun retrieveLinkDataFromTable(tableId: String): MutableMap<String, String> {
        val foundDataMap = mutableMapOf<String, String>()

        val anchor = classDocument.selectFirst("a[id=$tableId]")
            ?: classDocument.selectFirst("a[name=$tableId]")
            ?: return foundDataMap

        val blockList = anchor.parent()

        val tableBody = blockList.selectFirst("tbody")

        tableBody.select("th.colSecond")
            .filter { it.attr("scope").equals("row", true) }
            .forEach {
                val itemName = it.text()
                val anchorHref = it.selectFirst("a").attr("href")

                val itemUrl = "${url.substringBeforeLast("/")}/$anchorHref"

                foundDataMap[itemName] = itemUrl
            }

        return foundDataMap
    }

    /**
     * Retrieves the name and class information of inherited items from a table
     *
     * @param tableId The id of the table the data is coming from
     * @return A map containing the name and class information of the the found data
     */
    private fun retrieveInheritedDataFromTable(tableId: String): MutableMap<String, ClassInformation> {
        val foundDataMap = mutableMapOf<String, ClassInformation>()

        val anchor = classDocument.selectFirst("a[id=$tableId]")
            ?: classDocument.selectFirst("a[name=$tableId]")
            ?: return foundDataMap

        val blockList = anchor.parent()

        val inheritedAnchorList = blockList.select("a").filter {
            val attr = it.attr("id") ?: it.attr("name") ?: return@filter false
            return@filter attr.contains("inherited", true)
        }

        val inheritedAnchor = if (inheritedAnchorList.isEmpty()) return foundDataMap else inheritedAnchorList[0]

        val inheritedBlockList = inheritedAnchor.parent()
        inheritedBlockList.select("a").filter { it.hasAttr("href") && !it.hasAttr("title") }
            .forEach {
                val className = inheritedBlockList.selectFirst("h3").selectFirst("a").text()
                val foundClass = jenkins.retrieveClass(className)

                foundDataMap[it.text()] = foundClass

            }

        return foundDataMap
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
                    ?: throw Exception("Failed to match method signature: $signature")
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

        nestedClassMap = retrieveLinkDataFromTable("nested.class.summary")
        nestedClassList = nestedClassMap.keys.toMutableList()

        methodMap = getItemList("method.detail")
        methodList = methodMap.keys.toMutableList()

        enumMap = getItemList("enum.constant.detail")
        enumList = enumMap.keys.toMutableList()

        fieldMap = getItemList("field.detail")
        fieldList = fieldMap.keys.toMutableList()

        inheritedMethodMap = retrieveInheritedDataFromTable("method.summary")
        inheritedMethodList = inheritedMethodMap.keys.toMutableList()
    }

    private fun retrieveDataFromCache(classInfo: ClassInformation) {
        name = classInfo.name
        description = classInfo.description
        rawDescription = classInfo.rawDescription
        extraInformation = classInfo.extraInformation
        rawExtraInformation = classInfo.rawExtraInformation

        nestedClassMap = classInfo.nestedClassMap
        nestedClassList = classInfo.nestedClassList

        methodMap = classInfo.methodMap
        methodList = classInfo.methodList

        enumMap = classInfo.enumMap
        enumList = classInfo.enumList

        fieldMap = classInfo.fieldMap
        fieldList = classInfo.fieldList

        inheritedMethodMap = classInfo.inheritedMethodMap
        inheritedMethodList = classInfo.inheritedMethodList
    }

}