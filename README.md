# JenkinsParser-Kotlin
A kotlin api, that supports java, used to easily parse jenkins javadocs
## Documentation

### Maven Dependency

<a href="https://github.com/TheForbiddenAi/JenkinsParser-Kotlin">
    <img src="https://img.shields.io/github/v/release/TheForbiddenAi/JenkinsParser-Kotlin?label=Latest%20Version" alt="Latest Version">
</a>

<br>

Make sure to replace **VERSION** with the version shown above

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
    <groupId>com.github.TheForbiddenAi</groupId>
    <artifactId>JenkinsParser-Kotlin</artifactId>
    <version>VERSION</version>
</dependency>
```


### Initialization
First, you must create an instance of `Jenkins`
```kotlin
val jenkins = Jenkins(url)
```

* url - The jenkins class list url or tree url 
(Example URLs: https://docs.oracle.com/en/java/javase/11/docs/api/allclasses.html or https://docs.oracle.com/en/java/javase/13/docs/api/overview-tree.html)

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