package me.theforbiddenai.jenkinsparserkotlin

import me.theforbiddenai.jenkinsparserkotlin.entities.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class Jenkins(private var url: String) {

    val classMap: Map<String, String> by lazy { retrieveClassList() }
    internal val baseURL: String by lazy {
        url.substring(0, url.lastIndexOf("/") + 1)
    }

    private val classCache = Cache<ClassInformation>()
    private val hiddenUnicodeRegex = "\\p{C}".toRegex()

    init {
        url = url.removeSuffix("/")
    }

    fun search(query: String): List<Information> {
        val modifiedQuery = query.replace("#", ".")
            .replace(hiddenUnicodeRegex, "")
            .removeSuffix(".")
            .toLowerCase()
        val foundInformation = mutableListOf<Information>()

        val queryArgs = modifiedQuery.split(".")

        val foundClassList = searchClasses(queryArgs[0])

        if (foundClassList.isEmpty()) {
            throw Exception("Unable to find a class for the query $query")
        }

        if (queryArgs.size == 1) {
            return foundClassList
        }

        val resultUrlMap = mutableMapOf<String, String>()

        for (index in queryArgs.indices) {
            val qArg = queryArgs[index]

            classMap.filter { (_, className) ->

                val compareName = if (className.contains(".") && index != 0) {
                    val previousIndex = className.count { ".".contains(it) }

                    if (previousIndex > index) {
                        qArg
                    } else {
                        val stringBuilder = StringBuilder()
                        for (prevIndex in 0..previousIndex) {
                            stringBuilder.append("${queryArgs[prevIndex]}.")
                        }

                        stringBuilder.removeSuffix(".")
                            .toString()
                            .trim()
                    }


                } else {
                    qArg
                }

                return@filter className.equals(compareName, true)
            }.forEach { (classUrl, className) ->
                resultUrlMap[classUrl] = className
            }
        }

        val classInfoList = resultUrlMap.map { (classUrl, _) -> ClassInformation(this, classUrl) }


        classInfoList.forEach {

            val lowercaseClassName = it.name.toLowerCase()
            val memberName = modifiedQuery.replace(lowercaseClassName, "").removePrefix(".")

            foundInformation.addAll(it.searchAll(memberName))
        }


        /*foundClassList.forEach {
            val foundClassInfo =
                it.locateNestedClass(queryArgs.toTypedArray().copyOfRange(1, queryArgs.size).toList())

            val className = foundClassInfo.name
                .substringBefore("<")
                .trim()
                .toLowerCase()

            val queryNoName = modifiedQuery.substringAfter(className)
                .removePrefix(".")
                .trim()

            if (queryArgs.size > 1) {
                val previousClassName = className.substringBeforeLast(".")
                val previousClassList = searchClasses(previousClassName)

                if (previousClassList.isNotEmpty()) {
                    var queryWithoutName = modifiedQuery.replace(className, "")
                        .removeSuffix(".")

                    if (queryWithoutName.isBlank()) {
                        queryWithoutName = className.substringAfterLast(".")
                    }

                    if (!previousClassName.equals(queryWithoutName, true)) {
                        val previousClass = previousClassList[0]

                        foundInformation.addAll(previousClass.searchAll(queryWithoutName))
                    }
                }
            }

            if (queryNoName.isEmpty()) {
                foundInformation.add(foundClassInfo)

                try {
                    val potentialInfo = modifiedQuery.replaceBeforeLast(".", "")
                        .removePrefix(".")
                        .trim()

                    val potentialClassInfo =
                        modifiedQuery.substringAfter(it.name.toLowerCase(), "")
                            .removePrefix(".")
                            .substringBefore(".", "")
                            .trim()

                    val foundPotentialClass = it.searchAllNestedClasses(potentialClassInfo)[0]
                    foundInformation.addAll(foundPotentialClass.searchAll(potentialInfo))
                } catch (ignored: Exception) {

                }
            } else {
                foundInformation.addAll(foundClassInfo.searchAll(queryNoName))
            }
        }*/

        return foundInformation

    }

    /**
     * Retrieves all classes with the given name
     *
     * @param className The name of the classes being searched for
     * @return The list of all found classes
     */
    fun searchClasses(className: String): List<ClassInformation> {
        val classInfoList: MutableList<ClassInformation> = mutableListOf()

        classMap.filter { (_, classListName) -> classListName.equals(className, true) }
            .forEach { (classUrl, _) ->
                classInfoList.add(ClassInformation(this, classUrl))
            }

        return classInfoList
    }

    /**
     * Retrieves the first class with the given name
     *
     * @param className The name of the class being searched for
     * @param limitedView Whether or not to retrieve the limited class view
     * @return The found class object
     * @throws Exception If the class is not found
     */
    private fun retrieveClass(className: String, limitedView: Boolean): ClassInformation {
        classMap.filter { (_, classListName) -> classListName.equals(className, true) }
            .forEach { (classUrl, _) ->
                return retrieveClassByUrl(classUrl, limitedView)
            }

        throw Exception("Failed to find a class with the name of $className")
    }

    internal fun retrieveClassByUrl(url: String, limitedView: Boolean = false): ClassInformation {
        return if (classCache.containsUrl(url)) {
            classCache.retrieveInformation(url)!!
        } else {
            val classInfo = ClassInformation(this, url, limitedView)
            classCache.addInformation(url, classInfo)

            classInfo
        }
    }

    fun retrieveClass(className: String): ClassInformation {
        return retrieveClass(className, false)
    }

    /**
     * Retrieves the all methods with the given name, in the specified class
     *
     * @param classInfo The class information object the methods are being searched for in
     * @param methodName The name of the methods being searched for
     * @return A list of all found methods
     */
    fun searchMethods(classInfo: ClassInformation, methodName: String): List<MethodInformation> {
        return classInfo.searchMethods(methodName)
    }

    fun searchMethods(className: String, methodName: String): List<MethodInformation> {
        return searchMethods(retrieveClass(className), methodName)
    }


    /**
     * Retrieves the first method with the given name, in the specified class
     *
     * @param classInfo The class information object the method is being searched for in
     * @param methodName The name of the method being searched for
     * @return The found method object
     */
    fun retrieveMethod(classInfo: ClassInformation, methodName: String): MethodInformation {
        try {
            return searchMethods(classInfo, methodName)[0]
        } catch (ex: Exception) {
            throw Exception("Failed to find a method with the name of $methodName in the class ${classInfo.name}!")
        }
    }

    fun retrieveMethod(className: String, methodName: String): MethodInformation {
        return retrieveMethod(retrieveClass(className), methodName)
    }

    /**
     * Retrieves the first enum with the given name, in the specified class
     *
     * @param classInfo The class information object the enum is being searched for in
     * @param enumName The name of the enum being searched for
     * @return The found enum object
     */
    fun retrieveEnum(classInfo: ClassInformation, enumName: String): EnumInformation {
        return classInfo.retrieveEnum(enumName);
    }

    fun retrieveEnum(className: String, enumName: String): EnumInformation {
        return retrieveEnum(retrieveClass(className), enumName)
    }

    /**
     * Retrieves the first field with the given name, in the specified class
     *
     * @param classInfo The class information object the enum is being searched for in
     * @param fieldName The name of the field being searched for
     * @return The found field object
     */
    fun retrieveField(classInfo: ClassInformation, fieldName: String): FieldInformation {
        return classInfo.retrieveField(fieldName);
    }

    fun retrieveField(className: String, fieldName: String): FieldInformation {
        return retrieveField(retrieveClass(className), fieldName)
    }

    /**
     * Pulls the names and urls of all the classes from the class list java doc page and adds them to a map
     */
    private fun retrieveClassList(): Map<String, String> {
        val urlList = mutableMapOf<String, String>()
        val classDocument = retrieveClassDocument()

        val anchorList = if (url.contains("allclasses")) {
            classDocument.select("a")
        } else {
            classDocument.selectFirst("div.contentContainer").select("a")
        }

        anchorList.forEach {
            val href = it.attr("href")
                .replace("../", "")
                .trim()

            val classUrl = baseURL + href
            val className = it.text()

            if (!urlList.contains(classUrl)) {
                urlList[classUrl] = className
            }
        }

        return urlList
    }

    /**
     * Retrieves the [Document] which contains the list of available classes/interfaces/etc
     *
     * @return The found [Document]
     * @throws Exception If the [Document] can not be retrieved
     */
    private fun retrieveClassDocument(): Document {
        var classDocument: Document? = null

        var retryCount = 0
        val maxRetries = 3
        var exception: Exception = Exception("Unable to connect to $url")

        while (retryCount < maxRetries && classDocument == null) {
            retryCount++
            try {
                classDocument = Jsoup.connect(url).maxBodySize(0).get()
                break
            } catch (ex: Exception) {
                if (retryCount == maxRetries) {
                    exception = ex
                }
            }
        }

        if (classDocument == null) {
            throw exception
        }

        return classDocument
    }

}