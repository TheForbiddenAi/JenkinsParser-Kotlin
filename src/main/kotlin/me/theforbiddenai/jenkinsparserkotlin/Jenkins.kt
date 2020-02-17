package me.theforbiddenai.jenkinsparserkotlin

import me.theforbiddenai.jenkinsparserkotlin.entities.*
import org.jsoup.Jsoup

class Jenkins(private var url: String) {

    private val classList = mutableListOf<String>()
    private val baseURL: String

    init {
        url = url.removeSuffix("/")
        baseURL = url.substring(0, url.lastIndexOf("/") + 1)

        initClassList()
    }

    fun search(query: String): List<Information> {
        var modifiedQuery = query.replace("#", ".").removeSuffix(".")
        val foundInformation = mutableListOf<Information>()

        var foundClassList = searchClasses(query)
        if (foundClassList.isNotEmpty()) {
            return foundClassList
        }


        val queryArgs = modifiedQuery.split(".")
        lateinit var classInfo: ClassInformation

        foundClassList = searchClasses(queryArgs[0])

        when (foundClassList.size) {
            1 -> {
                classInfo = foundClassList[0]

                val foundClassInfo =
                    classInfo.locateNestedClass(queryArgs.toTypedArray().copyOfRange(1, queryArgs.size).toList())
                modifiedQuery = modifiedQuery.substringAfter(foundClassInfo.name).trim().removePrefix(".")

                if (modifiedQuery.isEmpty()) {
                    foundInformation.add(foundClassInfo)
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
     * @return The found class object
     * @throws Exception If the class is not found
     */
    fun retrieveClass(className: String): ClassInformation {
        classList.filter {
            val foundClassName = it.substringAfterLast("/").removeSuffix(".html")
            return@filter foundClassName.equals(className, true)
        }.forEach {
           return ClassInformation(this, it)
        }

        throw Exception("Failed to find a class with the name of $className")
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
        return searchMethods(classInfo, methodName)[0]
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
    private fun initClassList() {
        val classDocument = Jsoup.connect(url).get()

        classDocument.select("a").stream()
            .forEach {
                val classUrl = baseURL + it.attr("href")

                classList.add(classUrl)
            }
    }

}