# JenkinsParser-Kotlin
A kotlin api, that supports java, used to easily parse jenkins javadocs
## Usage

### Initialization
First, you must create an instance of `Jenkins`
```kotlin
val jenkins = Jenkins(url)
```

* url - The jenkins class list url (Example URL: https://docs.oracle.com/en/java/javase/11/docs/api/allclasses.html)

### Usage

Querying classes, methods, enums, and fields:
```kotlin
val infoList = jenkins.search("String")

val classInfo = jenkins.retrieveClass("String")
val methodInfo = jenkins.retrieveMethod("String", "valueOf")
// If you already have a class information object defined you can use:
val methodInfoWithClass = jenkins.retrieveMethod(classInfo, "valueOf")
// This works for fields and enums as well

val enumInfo = jenkins.retrieveEnum("Component.BaselineResizeBehavior", "center_offset")
val fieldInfo = jenkins.retrieveField("String", "case_insensitive_order")
```

When searching for multiple classes, or methods, with the same name use:
```kotlin
val classes = jenkins.searchClasses("Object")
val methods = jenkins.searchMethods("String", "valueOf")

// Or you can use the search method
val infoList = jenkins.search("String.valueOf")
```

When searching for something in a class where you may not know if it is a method, enum, or field use:
```kotlin
val classInfo = jenkins.retrieveClass("String")
val infoList = classInfo.searchAll("valueOf")
```