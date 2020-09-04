package me.theforbiddenai.jenkinsparserkotlin.entities

import me.theforbiddenai.jenkinsparserkotlin.Cache
import me.theforbiddenai.jenkinsparserkotlin.Jenkins
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

private val hiddenUnicodeRegex = "\\p{C}".toRegex()
private val annotationRegex = "[@]\\w+\\s+".toRegex()
private val methodNameRegex = "\\w+\\s?\\(.*\\)".toRegex()
private val whitespaceRegex = "\\s+".toRegex()

data class ClassInformation internal constructor(
    val jenkins: Jenkins,
    override val url: String,
    val limitedView: Boolean = false
) : Information() {

    private val classDocument = Jsoup.connect(url).get()

    private val methodCache = Cache<MethodInformation>()
    private val enumCache = Cache<EnumInformation>()
    private val fieldCache = Cache<FieldInformation>()

    override lateinit var name: String
    override lateinit var type: String
    override lateinit var description: String
    override lateinit var rawDescription: String
    override var extraInformation: MutableMap<String, String> = mutableMapOf()
    override var rawExtraInformation: MutableMap<String, String> = mutableMapOf()

    private var nestedClassMap: MutableMap<String, String> = mutableMapOf()
    private var methodMap: MutableMap<String, Element> = mutableMapOf()
    private var enumMap: MutableMap<String, Element> = mutableMapOf()
    private var fieldMap: MutableMap<String, Element> = mutableMapOf()

    var nestedClassList: MutableList<String> = mutableListOf()
    var methodList: MutableList<String> = mutableListOf()
    var enumList: MutableList<String> = mutableListOf()
    var fieldList: MutableList<String> = mutableListOf()

    private var inheritedNestedClassMap: MutableMap<String, String> = mutableMapOf()
    private var inheritedMethodMap: MutableMap<String, String> = mutableMapOf()
    private var inheritedEnumMap: MutableMap<String, String> = mutableMapOf()
    private var inheritedFieldMap: MutableMap<String, String> = mutableMapOf()

    var inheritedNestedClassList: MutableList<String> = mutableListOf()
    var inheritedMethodList: MutableList<String> = mutableListOf()
    var inheritedEnumList: MutableList<String> = mutableListOf()
    var inheritedFieldList: MutableList<String> = mutableListOf()

    init {
        if (limitedView) {
            retrieveLimitedView()
        } else {
            retrieveDataFromDocument()
        }

    }

    /**
     * Searches through ALL methods, enums, and fields looking for a specified query
     *
     * @param query The name of the information being looked for
     * @return Returns a list of all of the found information
     */
    fun searchAll(query: String): List<Information> {
        val foundInformation = mutableListOf<Information>()

        foundInformation.addAll(searchAllMethods(query))
        foundInformation.addAll(searchAllEnums(query))
        foundInformation.addAll(searchAllFields(query))

        return foundInformation
    }

    /**
     * Retrieves all nested classes defined in the class and all inherited nested classes with a specified name
     *
     * @param query The name of the classes being searched for
     * @param limitedView Whether or not to retrieve the limited class view
     * @return Returns a list of all found classes
     */
    private fun searchAllNestedClasses(query: String, limitedView: Boolean): List<ClassInformation> {
        val foundNestedClasses: MutableList<ClassInformation> = mutableListOf()

        foundNestedClasses.addAll(handleInfoNotFoundError(query) { retrieveNestedClass(it, limitedView) })
        foundNestedClasses.addAll(handleInfoNotFoundError(query) { retrieveInheritedNestedClass(it, limitedView) })

        return foundNestedClasses
    }

    fun searchAllNestedClasses(query: String): List<ClassInformation> {
        return searchAllNestedClasses(query, false)
    }

    /**
     * Retrieves a nested class with the specified name
     *
     * @param query The name of the nested class being searched for
     * @param limitedView Whether or not to retrieve the limited class view
     * @return Returns the found nested classes
     * @throws Exception If the nested class is not found
     */
    private fun retrieveNestedClass(query: String, limitedView: Boolean): ClassInformation {
        if (nestedClassList.containsIgnoreCase(query)) {
            val modifiedQuery = query.replace("$name.", "").trim()
            nestedClassMap.filter { (className, _) -> className.equals(modifiedQuery, true) }
                .forEach { (_, classUrl) ->
                    return jenkins.retrieveClassByUrl(classUrl, limitedView)
                }

        }
        throw Exception("Could not find a nested class with the name $query in $name")
    }

    fun retrieveNestedClass(query: String): ClassInformation {
        return retrieveNestedClass(query, false)
    }

    /**
     * Retrieves an inherited nested class with the specified name
     *
     * @param query The name of the inherited nested class being searched for
     * @param limitedView Whether or not to retrieve the limited class view
     * @return Returns the found inherited nested classes
     * @throws Exception If the inherited nested class is not found
     */
    private fun retrieveInheritedNestedClass(query: String, limitedView: Boolean): ClassInformation {
        if (inheritedNestedClassList.containsIgnoreCase(query)) {
            val foundNestedClasses: List<ClassInformation> =
                retrieveInheritedData(inheritedNestedClassMap, query, limitedView)
            if (foundNestedClasses.isNotEmpty()) return foundNestedClasses[0]
        }

        throw Exception("Could not find an inherited nested class/interface with the name of $query in the class $name")
    }

    fun retrieveInheritedNestedClass(query: String): ClassInformation {
        return retrieveInheritedNestedClass(query, false)
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

            val methodInfo = if (methodCache.containsUrl(methodUrl)) {
                methodCache.retrieveInformation(methodUrl)!!
            } else {
                MethodInformation(this, fieldElement, methodUrl, methodName)
            }
            methodCache.addInformation(methodUrl, methodInfo)
            foundMethodList.add(methodInfo)
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
        if (enumList.containsIgnoreCase(query)) {
            retrieveData(enumMap, query).forEach { (enumName, enumUrl) ->
                val fieldElement = enumMap[enumName] ?: return@forEach

                return if (enumCache.containsUrl(enumUrl)) {
                    enumCache.retrieveInformation(enumUrl)!!
                } else {
                    val enumInfo = EnumInformation(this, fieldElement, enumUrl, enumName)
                    enumCache.addInformation(enumUrl, enumInfo)

                    enumInfo
                }

            }
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
        if (inheritedEnumList.containsIgnoreCase(query)) {
            val foundEnums: List<EnumInformation> = retrieveInheritedData(inheritedEnumMap, query)
            if (foundEnums.isNotEmpty()) return foundEnums[0]
        }

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
        if (fieldList.containsIgnoreCase(query)) {
            retrieveData(fieldMap, query).forEach { (fieldName, fieldUrl) ->
                val fieldElement = fieldMap[fieldName] ?: return@forEach

                return if (fieldCache.containsUrl(fieldUrl)) {
                    fieldCache.retrieveInformation(fieldUrl)!!
                } else {
                    val fieldInfo = FieldInformation(this, fieldElement, fieldUrl, fieldName)
                    fieldCache.addInformation(fieldUrl, fieldInfo)

                    fieldInfo
                }


            }
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
        if (inheritedFieldList.containsIgnoreCase(query)) {
            val foundFields: List<FieldInformation> = retrieveInheritedData(inheritedFieldMap, query)
            if (foundFields.isNotEmpty()) return foundFields[0]
        }

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

                // This allows for backwards compatibility with old jenkins versions
                var anchorElement = infoElement.parent().previousElementSibling() ?: return@forEach
                val tagName = anchorElement.tagName() ?: ""

                if (tagName.contentEquals("h2")) {
                    anchorElement = infoElement.selectFirst("a")
                }

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
        inheritedMap: MutableMap<String, String>,
        query: String,
        limitedClassView: Boolean = false
    ): List<T> {
        val foundInfoMap = mutableListOf<Information>()

        inheritedMap.filter { (infoName, _) -> infoName.equals(query, true) }
            .forEach { (infoName, classUrl) ->
                val classInformation = jenkins.retrieveClassByUrl(classUrl, limitedClassView)

                when (inheritedMap) {
                    inheritedNestedClassMap -> foundInfoMap.add(classInformation)
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

        val tableRows = blockList.select("tr") ?: return foundDataMap

        tableRows.select(".colSecond")
            .filter {
                it.attr("scope").equals("row", true)
            }
            .forEach {
                val className = name.substringBefore("<")
                val itemName = it.text()
                    .substringAfter("$className.")
                    .substringBefore("<")

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
    private fun retrieveInheritedDataFromTable(tableId: String): MutableMap<String, String> {
        val foundDataMap = mutableMapOf<String, String>()

        val anchor = classDocument.selectFirst("a[id=$tableId]")
            ?: classDocument.selectFirst("a[name=$tableId]")
            ?: return foundDataMap


        val blockList = anchor.parent()

        val inheritedAnchorList = blockList.select("a").filter {
            val attr = if (it.hasAttr("id")) it.attr("id") else it.attr("name")
            return@filter attr.contains("inherited", true)
        }

        inheritedAnchorList.forEach { foundAnchor ->
            val parentElement = foundAnchor.parent();

            val inheritedList =
                parentElement.selectFirst("li.blocklist") ?: parentElement.selectFirst("div.inheritedList")

            val headerElement = inheritedList.selectFirst("h3") ?: inheritedList.selectFirst("h2")
            val headerAnchorElement = headerElement.selectFirst("a")

            val href = headerAnchorElement.attr("href")
            val newUrl = getUrl(href)

            if (tableId.equals("nested.class.summary", true)) {
                val className = headerAnchorElement.text()
                foundDataMap[className] = newUrl
            } else {
                val itemList = inheritedList.selectFirst("code").select("a")
                itemList.forEach {
                    foundDataMap[it.text()] = newUrl
                }
            }

        }

        return foundDataMap
    }

    private fun getUrl(href: String): String {
        val strippedName = name.substringBefore("<")
        val packageUrl = url.substringBefore(strippedName)

        return with(href) {
            when {
                contains("http") -> href
                contains("../") -> jenkins.baseURL + href.replace("^[^A-Za-z]+".toRegex(), "")
                contains("#") || contains("/") || contains(".html") -> packageUrl + href
                contains("/") -> packageUrl + href
                else -> url.replace("$name.html", "") + href
            }
        }

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

            val itemName = if (listId.equals("method.detail", true)) {
                val signature = it.selectFirst(".memberSignature")?.text() ?: it.selectFirst("pre").text()

                val modifiedSignature = signature.replace(hiddenUnicodeRegex, " ")
                    .replace(annotationRegex, "")
                    .replace(whitespaceRegex, " ")
                    .substringAfter("public")
                    .trim()

                methodNameRegex.find(modifiedSignature)?.value
                    ?: throw Exception("Failed to match method signature: $signature")
            } else {
                it.selectFirst("h4")?.text() ?: it.selectFirst("h3").text()
            }


            returnMap[itemName] = it
        }

        return returnMap
    }

    /**
     * Goes to the deepest (inherited) nested class
     *
     * @param queryArgs The list of arguments potentially containing (inherited) nested class name
     * @return The found class information
     */
    internal fun locateNestedClass(queryArgs: List<String>): ClassInformation {
        var classInfo = this

        for (arg in queryArgs) {
            if (checkForNestedClass(classInfo, arg) != classInfo) {
                classInfo = checkForNestedClass(classInfo, arg)
            } else break
        }

        if (classInfo == this) {
            val fullQuery = queryArgs.joinToString(separator = ".")

            if (checkForNestedClass(classInfo, fullQuery) != classInfo) return checkForNestedClass(classInfo, fullQuery)

            val partialQuery = fullQuery.substringBeforeLast(".")

            classInfo = checkForNestedClass(classInfo, partialQuery)
        }
        classInfo.retrieveDataFromDocument()
        return classInfo
    }

    /**
     * Checks if there is a nested class by a specified name and if so returns that class
     *
     * @param classInfo The class the nested class is coming from
     * @param query The potential nested class's name
     * @return The found class information or the given class information if none is found
     */
    private fun checkForNestedClass(classInfo: ClassInformation, query: String): ClassInformation {
        val nestedClassList = classInfo.nestedClassList
        val inheritedNestedClassList = classInfo.inheritedNestedClassList

        if (nestedClassList.containsIgnoreCase(query) || inheritedNestedClassList.containsIgnoreCase(query)) {
            return classInfo.searchAllNestedClasses(query, true)[0]
        }
        return classInfo
    }

    private fun retrieveLimitedView() {
        val fullName = classDocument.selectFirst(".title")?.text() ?: "N/A"
        val nameArgs = fullName.split("\\s+".toRegex())

        type = if (nameArgs.size > 1) nameArgs[0] else "Class"
        name = if (nameArgs.size > 1) nameArgs[1] else nameArgs[0]

        nestedClassMap = retrieveLinkDataFromTable("nested.class.summary")
        nestedClassList = nestedClassMap.keys.toMutableList()

        inheritedNestedClassMap = retrieveInheritedDataFromTable("nested.class.summary")
        inheritedNestedClassList = inheritedNestedClassMap.keys.toMutableList()
    }

    private fun retrieveDataFromDocument() {
        val fullName = classDocument.selectFirst(".title")?.text() ?: "N/A"
        val args = fullName.split("\\s+".toRegex()).toTypedArray()

        type = if (args.size > 1) args[0] else "Class"
        name = if (args.size > 1) {
            args.copyOfRange(1, args.size)
                .joinToString(" ")
        } else fullName

        val descriptionElement = classDocument.selectFirst(".description")
            .selectFirst(".block")

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

}