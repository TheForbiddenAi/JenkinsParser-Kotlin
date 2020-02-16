package me.theforbiddenai.jenkinsparserkotlin

import me.theforbiddenai.jenkinsparserkotlin.entities.*
import org.jsoup.Jsoup

class Jenkins(private var url: String) {

    private val classList = mutableMapOf<String, String>()
    private val baseURL: String

    init {
        url = url.removeSuffix("/")
        baseURL = url.substring(0, url.lastIndexOf("/") + 1)

        initClassList()
    }

    fun search(query: String): List<Information> {
        TODO("Not implemented")
    }

    /**
     * Retrieves all classes with the given name
     *
     * @param className The name of the classes being searched for
     * @return The list of all found classes
     */
    fun searchClasses(className: String): List<ClassInformation> {
        val classInfoList: MutableList<ClassInformation> = mutableListOf()

        classList.keys.filter { it.equals(className, true) }
            .forEach {
                val classUrl = classList[it] ?: return@forEach
                classInfoList.add(ClassInformation(classUrl, className.toLowerCase()))
            }

        return classInfoList
    }

    /**
     * Retrieves the first class with the given name
     *
     * @param className The name of the class being searched for
     * @return The found class object
     */
    fun retrieveClass(className: String): ClassInformation {
        val classUrl = classList[className.toLowerCase()]
            ?: throw Exception("Failed to find a class with the name of $className")

        return ClassInformation(classUrl, className.toLowerCase())
    }

    /**
     * Retrieves the all methods with the given name, in the specified class
     *
     * @param classInfo The class information object the methods are being searched for in
     * @param methodName The name of the methods being searched for
     * @return A list of all found methods
     */
    fun searchMethods(classInfo: ClassInformation, methodName: String): List<MethodInformation> {
        return classInfo.retrieveMethods(methodName)
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

        classDocument.select("li").stream()
            .filter { it.selectFirst("a") != null }
            .forEach {
                val className = it.text().toLowerCase()
                val classUrl = baseURL + it.selectFirst("a").attr("href")

                classList[className] = classUrl
            }
    }

}