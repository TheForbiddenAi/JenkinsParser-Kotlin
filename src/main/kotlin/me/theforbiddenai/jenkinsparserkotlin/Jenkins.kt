package me.theforbiddenai.jenkinsparserkotlin

import me.theforbiddenai.jenkinsparserkotlin.entities.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.math.max

class Jenkins(private var url: String) {

    val classList: MutableList<String> by lazy { retrieveClassList() }
    internal val baseURL: String by lazy {
        url.substring(0, url.lastIndexOf("/") + 1)
    }

    private val classCache = Cache<ClassInformation>()
    private val hiddenUnicodeRegex = "\\p{C}".toRegex()

    init {
        url = url.removeSuffix("/")
    }

    fun search(query: String): List<Information> {
        var modifiedQuery = query.replace("#", ".")
            .replace(hiddenUnicodeRegex, "")
            .removeSuffix(".")
            .toLowerCase()
        val foundInformation = mutableListOf<Information>()

        val queryArgs = modifiedQuery.split(".")
        lateinit var classInfo: ClassInformation

        val foundClassList = searchClasses(queryArgs[0])

        if (queryArgs.size == 1) {
            return foundClassList
        }

        when (foundClassList.size) {
            1 -> {
                classInfo = foundClassList[0]

                val foundClassInfo =
                    classInfo.locateNestedClass(queryArgs.toTypedArray().copyOfRange(1, queryArgs.size).toList())

                val oldQuery = modifiedQuery

                val className = foundClassInfo.name.substringBefore("<").removePrefix("<").trim().toLowerCase()
                modifiedQuery = modifiedQuery.substringAfter(className).removePrefix(".").trim()

                val previousClassName = className.substringBeforeLast(".")
                val previousClassList = searchClasses(previousClassName)

                if (previousClassList.isNotEmpty()) {
                    val queryWithoutName = className.substringAfterLast(".")
                    val previousClass = previousClassList[0]

                    foundInformation.addAll(previousClass.searchAll(queryWithoutName))
                }

                if (modifiedQuery.isEmpty()) {
                    foundInformation.add(foundClassInfo)

                    try {
                        val potentialInfo = oldQuery.replaceBeforeLast(".", "").removePrefix(".").trim()
                        val potentialClassInfo =
                            oldQuery.substringAfter(classInfo.name.toLowerCase(), "").removePrefix(".")
                                .substringBefore(".", "").trim()

                        val foundPotentialClass = classInfo.searchAllNestedClasses(potentialClassInfo)[0]
                        foundInformation.addAll(foundPotentialClass.searchAll(potentialInfo))
                    } catch (ignored: Exception) {

                    }
                } else {
                    foundInformation.addAll(foundClassInfo.searchAll(modifiedQuery))
                    if (foundInformation.isEmpty()) {
                        throw Exception("Unable to find a method, enum, or field for the query $query")
                    }
                }
            }
            0 -> {
                throw Exception("Unable to find a class for the query $query")
            }
            else -> {
                foundClassList.forEach {
                    val foundClassInfo =
                        it.locateNestedClass(queryArgs.toTypedArray().copyOfRange(1, queryArgs.size).toList())
                    val queryWithoutName = modifiedQuery.substringAfter(foundClassInfo.name).trim().removePrefix(".")

                    if (queryWithoutName.isEmpty()) {
                        foundInformation.add(foundClassInfo)
                    } else {
                        foundInformation.addAll(foundClassInfo.searchAll(queryWithoutName))
                    }
                }
            }
        }

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

        classList.filter {
            val foundClassName = it.substringAfterLast("/").removeSuffix(".html")
            return@filter foundClassName.equals(className, true)
        }.forEach {
            classInfoList.add(ClassInformation(this, it))
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
        classList.filter {
            val foundClassName = it.substringAfterLast("/").removeSuffix(".html")
            return@filter foundClassName.equals(className, true)
        }.forEach { classUrl ->
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
    private fun retrieveClassList(): MutableList<String> {
        val urlList = mutableListOf<String>()

        var classDocument: Document? = null

        var retryCount = 0
        val maxRetries = 3
        var connected = false
        var exception: Exception = Exception("Unable to connect to $url")

        while (retryCount < maxRetries && classDocument == null) {
            retryCount++
            try {
                classDocument = Jsoup.connect(url).maxBodySize(0).get()
                connected = true
                break
            } catch (ex: Exception) {
                if (retryCount == maxRetries) {
                    exception = ex
                }
            }
        }

        if (!connected || classDocument == null) {
            throw exception
        }

        val elementToSearch = if (url.contains("allclasses")) "a" else "li.circle"

        classDocument.select(elementToSearch).stream()
            .forEach {

                var href = if (it.hasAttr("href")) {
                    it.attr("href")
                } else {
                    it.selectFirst("a").attr("href")
                }

                href = href.replace("../", "").trim()
                val classUrl = baseURL + href

                if (!urlList.contains(classUrl)) urlList.add(classUrl)
            }

        return urlList
    }

}