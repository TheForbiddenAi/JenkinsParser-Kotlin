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

    private var inheritedNestedClassMap: MutableMap<String, ClassInformation> = mutableMapOf()
    private var inheritedMethodMap: MutableMap<String, ClassInformation> = mutableMapOf()
    private var inheritedEnumMap: MutableMap<String, ClassInformation> = mutableMapOf()
    private var inheritedFieldMap: MutableMap<String, ClassInformation> = mutableMapOf()

    var inheritedNestedClassList: MutableList<String> = mutableListOf()
    var inheritedMethodList: MutableList<String> = mutableListOf()
    var inheritedEnumList: MutableList<String> = mutableListOf()
    var inheritedFieldList: MutableList<String> = mutableListOf()

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

    fun searchAll (query: String): List<Information> {
        TODO("NOT IMPLEMENTED")
    }

    /**
     * Retrieves all nested classes defined in the class and all inherited nested classes with a specified name
     *
     * @param query The name of the classes being searched for
     * @return Returns a list of all found classes
     */
    fun searchAllNestedClasses(query: String): List<ClassInformation> {
        val foundNestedClasses: MutableList<ClassInformation> = mutableListOf()

        foundNestedClasses.addAll(handleInfoNotFoundError(query) { retrieveNestedClass(it) })
        foundNestedClasses.addAll(handleInfoNotFoundError(query) { retrieveInheritedNestedClass(it) })

        return foundNestedClasses
    }

    /**
     * Retrieves a nested class with the specified name
     *
     * @param query The name of the nested class being searched for
     * @return Returns the found nested classes
     * @throws Exception If the nested class is not found
     */
    fun retrieveNestedClass(query: String): ClassInformation {
        nestedClassMap.filter { (className, _) ->
            val modifiedClassName = className.replace("$name.", "").trim()
            val modifiedQuery = query.replace("$name.", "").trim()

            modifiedClassName.equals(modifiedQuery, true)
        }.forEach { (className, classUrl) -> return ClassInformation(jenkins, classUrl, className) }

        throw Exception("Could not find a nested class with the name $query in $name")
    }

    /**
     * Retrieves an inherited nested class with the specified name
     *
     * @param query The name of the inherited nested class being searched for
     * @return Returns the found inherited nested classes
     * @throws Exception If the inherited nested class is not found
     */
    fun retrieveInheritedNestedClass(query: String): ClassInformation {
        val foundNestedClasses: List<ClassInformation> = retrieveInheritedData(inheritedNestedClassMap, query)
        if (foundNestedClasses.isNotEmpty()) return foundNestedClasses[0]

        throw Exception("Could not find an inherited nested class/interface with the name of $query in the class $name")
    }

    /**
     * Retrieves all methods defined in the class and all inherited methods with a specified name
     *
     * @param query The name of the methods being searched for
     * @return Returns a list of all found methods
     */
    fun searchAllMethods(query: String): List<MethodInformation> {
        val foundMethodList: MutableList<MethodInformation> = mutableListOf()

        foundMethodList.addAll(searchMethods(query))
        foundMethodList.addAll(searchInheritedMethods(query))

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
        return retrieveInheritedData(inheritedMethodMap, query)
    }

    /**
     * Retrieves all enums defined in the class and all inherited enums with a specified name
     *
     * @param query The name of the enums being searched for
     * @return Returns a list of all found enums
     */
    fun searchAllEnums(query: String): List<EnumInformation> {
        val foundEnumList: MutableList<EnumInformation> = mutableListOf()

        foundEnumList.addAll(handleInfoNotFoundError(query) { retrieveEnum(it) })
        foundEnumList.addAll(handleInfoNotFoundError(query) { retrieveInheritedEnum(it) })

        return foundEnumList
    }

    /**
     * Retrieves the enum with a specified name
     *
     * @param query The name of the enum being searched for
     * @return Returns the found enum
     * @throws Exception If the enum is not found
     */
    fun retrieveEnum(query: String): EnumInformation {
        retrieveData(enumMap, query).forEach { (enumName, enumUrl) ->
            val fieldElement = enumMap[enumName] ?: return@forEach
            return EnumInformation(this, fieldElement, enumUrl, enumName)
        }

        throw Exception("Could not find an enum with the name of $query in the class $name")
    }

    /**
     * Retrieves an inherited enum in the class with a specified name
     *
     * @param query The name of the inherited enum being searched for
     * @return Returns the found inherited enum
     * @throws Exception If the inherited enum is not found
     */
    fun retrieveInheritedEnum(query: String): EnumInformation {
        val foundEnums: List<EnumInformation> = retrieveInheritedData(inheritedEnumMap, query)
        if (foundEnums.isNotEmpty()) return foundEnums[0]

        throw Exception("Could not find an inherited enum with the name of $query in the class $name")
    }

    /**
     * Retrieves all fields defined in the class and all inherited fields with a specified name
     *
     * @param query The name of the fields being searched for
     * @return Returns a list of all found fields
     */
    fun searchAllFields(query: String): List<FieldInformation> {
        val foundFieldList: MutableList<FieldInformation> = mutableListOf()

        foundFieldList.addAll(handleInfoNotFoundError(query) { retrieveField(it) })
        foundFieldList.addAll(handleInfoNotFoundError(query) { retrieveInheritedField(it) })

        return foundFieldList
    }

    /**
     * Retrieves the field with a specified name
     *
     * @param query The name of the fields being searched for
     * @return Returns the found field
     * @throws Exception If the field is not found
     */
    fun retrieveField(query: String): FieldInformation {
        retrieveData(fieldMap, query).forEach { (fieldName, fieldUrl) ->
            val fieldElement = fieldMap[fieldName] ?: return@forEach
            return FieldInformation(this, fieldElement, fieldUrl, fieldName)
        }

        throw Exception("Could not find a field with the name of $query in the class $name")
    }

    /**
     * Retrieves an inherited field in the class with a specified name
     *
     * @param query The name of the inherited field being searched for
     * @return Returns the found inherited field
     * @throws Exception If the inherited field is not found
     */
    fun retrieveInheritedField(query: String): FieldInformation {
        val foundFields: List<FieldInformation> = retrieveInheritedData(inheritedFieldMap, query)
        if (foundFields.isNotEmpty()) return foundFields[0]

        throw Exception("Could not find an inherited field with the name of $query in the class $name")
    }

    /**
     * Retrieves the name and url from a query of a map (this does not include the inherited maps)
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

                val anchorAttr = if (anchorElement.hasAttr("id")) {
                    anchorElement.attr("id")
                } else anchorElement.attr("name")

                val infoUrl = "$url#${anchorAttr}"

                foundDataMap[infoName] = infoUrl
            }

        return foundDataMap
    }

    /**
     * Retrieves the information object from a query of an inherited map
     *
     * @param inheritedMap The inherited map being searched in
     * @param query The information being searched for
     * @return A list of all found information objects
     * @throws Exception If the map is unable to match one of the inherited maps
     */
    private fun <T : Information> retrieveInheritedData(
        inheritedMap: MutableMap<String, ClassInformation>,
        query: String
    ): List<T> {
        val foundInfoMap = mutableListOf<Information>()

        inheritedMap.filter { (infoName, _) -> infoName.equals(query, true) }
            .forEach { (infoName, classInformation) ->
                when (inheritedMap) {
                    inheritedNestedClassMap -> foundInfoMap.add(jenkins.retrieveClass(infoName))
                    inheritedMethodMap -> foundInfoMap.addAll(classInformation.searchMethods(infoName))
                    inheritedEnumMap -> foundInfoMap.add(classInformation.retrieveEnum(infoName))
                    inheritedFieldMap -> foundInfoMap.add(classInformation.retrieveField(infoName))
                    else -> throw Exception("Failed to match inherited map")
                }
            }


        return foundInfoMap as List<T>
    }

    /**
     * Handles errors caused when one of the retrieve functions is unable to find the info,
     * this function is used for only the search all functions
     *
     * @param query The information being searched for
     * @param retrieveInfo The retrieve function to fetch the information from
     * @return A list of all found information
     */
    private fun <T : Information> handleInfoNotFoundError(query: String, retrieveInfo: (String) -> T): List<T> {
        return try {
            mutableListOf(retrieveInfo(query))
        } catch (ex: Exception) {
            mutableListOf()
        }
    }

    /**
     * Retrieves the name and url of items from a table, only used for retrieving nested classes
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

        val tableBody = blockList.selectFirst("tbody") ?: return foundDataMap

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
            val attr = if (it.hasAttr("id")) it.attr("id") else it.attr("name")
            return@filter attr.contains("inherited", true)
        }

        inheritedAnchorList.forEach { foundAnchor ->
            val inheritedBlockList = foundAnchor.parent().selectFirst("li.blocklist")
            val className = inheritedBlockList.selectFirst("h3").selectFirst("a").text()

            try {
                val foundClass = jenkins.retrieveClass(className)

                val itemList = inheritedBlockList.selectFirst("code")
                itemList.select("a").forEach {
                    foundDataMap[it.text()] = foundClass
                }
            } catch (ex: Exception) {
                return@forEach
            }

        }

        return foundDataMap
    }

    /**
     * Gets the block list by getting an anchor's parent and turns the information into a map
     *
     * @param listId The anchor's id
     * @return A map of the name and element of the found information
     * @throws Exception If the method name regex is unable to match the method signature (this is for the method.detail list only)
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
        methodMap = getItemList("method.detail")
        enumMap = getItemList("enum.constant.detail")
        fieldMap = getItemList("field.detail")

        nestedClassList = nestedClassMap.keys.toMutableList()
        methodList = methodMap.keys.toMutableList()
        enumList = enumMap.keys.toMutableList()
        fieldList = fieldMap.keys.toMutableList()

        inheritedNestedClassMap = retrieveInheritedDataFromTable("nested.class.summary")
        inheritedMethodMap = retrieveInheritedDataFromTable("method.summary")
        inheritedEnumMap = retrieveInheritedDataFromTable("enum.constant.summary")
        inheritedFieldMap = retrieveInheritedDataFromTable("field.summary")

        inheritedNestedClassList = inheritedNestedClassMap.keys.toMutableList()
        inheritedMethodList = inheritedMethodMap.keys.toMutableList()
        inheritedEnumList = inheritedEnumMap.keys.toMutableList()
        inheritedFieldList = inheritedFieldMap.keys.toMutableList()
    }

    private fun retrieveDataFromCache(classInfo: ClassInformation) {
        name = classInfo.name
        description = classInfo.description
        rawDescription = classInfo.rawDescription
        extraInformation = classInfo.extraInformation
        rawExtraInformation = classInfo.rawExtraInformation

        nestedClassMap = classInfo.nestedClassMap
        methodMap = classInfo.methodMap
        enumMap = classInfo.enumMap
        fieldMap = classInfo.fieldMap

        nestedClassList = classInfo.nestedClassList
        methodList = classInfo.methodList
        enumList = classInfo.enumList
        fieldList = classInfo.fieldList

        inheritedNestedClassMap = classInfo.inheritedNestedClassMap
        inheritedMethodMap = classInfo.inheritedMethodMap
        inheritedEnumMap = classInfo.inheritedEnumMap
        inheritedFieldMap = classInfo.inheritedFieldMap

        inheritedNestedClassList = classInfo.inheritedNestedClassList
        inheritedMethodList = classInfo.inheritedMethodList
        inheritedFieldList = classInfo.inheritedFieldList
        inheritedFieldList = classInfo.inheritedFieldList
    }

}