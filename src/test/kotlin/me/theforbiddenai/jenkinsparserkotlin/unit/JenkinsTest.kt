package me.theforbiddenai.jenkinsparserkotlin.unit

import io.mockk.*
import me.theforbiddenai.jenkinsparserkotlin.Jenkins
import me.theforbiddenai.jenkinsparserkotlin.entities.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JenkinsTest {

    private val jenkinsMock: Jenkins = mockk()
    private var jenkinsSpy = spyk(jenkinsMock)

    @BeforeEach
    fun init() {
        clearMocks(jenkinsMock)
        jenkinsSpy = spyk(jenkinsMock)
    }

    @Nested
    inner class SearchTests {

        private val classInfoMock: ClassInformation = mockk()
        private val methodMock: MethodInformation = mockk()
        private val enumMock: EnumInformation = mockk()
        private val fieldMock: FieldInformation = mockk()

        @BeforeEach
        fun init() {
            every { jenkinsSpy.classMap } returns mapOf(
                Pair("Class1", "String"),
                Pair("Class2", "Integer"),
                Pair("DClass1", "List"),
                Pair("DClass2", "list"),
                Pair("Inherit1", "Component.AccessibleAWTComponent"),
                Pair("Inherit2", "Component.AccessibleAWTComponent.AccessibleAWTComponentHandler"),
            )

            jenkinsSpy.classMap.forEach { (url, name) ->
                val members = when (name) {
                    "Component.AccessibleAWTComponent" -> {
                        createMemberSpyList(Pair("accessibleAWTComponentHandler", "field"))
                    }
                    "String" -> {
                        createMemberSpyList(
                            Pair("method", "method")
                        )
                    }
                    "Integer" -> {
                        createMemberSpyList(
                            Pair("member", "method"),
                            Pair("member", "field")
                        )
                    }
                    else -> emptyList()
                }



                createClassSpy(name, url, members)
            }
        }

        @Test
        fun `search classes - single result`() {
            val results = jenkinsSpy.search("String")

            verifyOrder {
                jenkinsSpy.search("String")
                jenkinsSpy.retrieveClassByUrl("Class1", false)
            }

            assertThat(results.size).isEqualTo(1)
            assertThat(results[0].name).isEqualTo("String")
        }

        @Test
        fun `search classes - multiple results`() {
            val results = jenkinsSpy.search("List")

            verifyOrder {
                jenkinsSpy.search("List")
                jenkinsSpy.retrieveClassByUrl("DClass1", false)
                jenkinsSpy.retrieveClassByUrl("DClass2", false)
            }

            assertThat(results.size).isEqualTo(2)
            assertThat(results[0].name).isEqualTo("List")
            assertThat(results[1].name).isEqualTo("list")
        }

        @Test
        fun `search class members - single result`() {
            val results = jenkinsSpy.search("String.method")

            verifyOrder {
                jenkinsSpy.search("String.method")
                jenkinsSpy.retrieveClassByUrl("Class1", false)
            }

            assertThat(results.size).isEqualTo(1)
            assertThat(results[0].name).isEqualTo("method")
        }

        @Test
        fun `search class members - multiple results`() {
            val results = jenkinsSpy.search("Integer.member")

            verifyOrder {
                jenkinsSpy.search("Integer.member")
                jenkinsSpy.retrieveClassByUrl("Class2", false)
            }

            assertThat(results.size).isEqualTo(2)
            assertThat(results[0].name).isEqualTo("member")
            assertThat(results[1].name).isEqualTo("member")
            assertThat(results[0]).isInstanceOf(MethodInformation::class.java)
            assertThat(results[1]).isInstanceOf(FieldInformation::class.java)
        }

        @Test
        fun `inheritance test`() {
            val results = jenkinsSpy.search("Component.AccessibleAWTComponent")

            verifyOrder {
                jenkinsSpy.search("Component.AccessibleAWTComponent")
                jenkinsSpy.retrieveClassByUrl("Inherit1", false)
            }

            assertThat(results.size).isEqualTo(1)
            assertThat(results[0].name).isEqualTo("Component.AccessibleAWTComponent")
        }

        @Test
        fun `multiple inherited results with same name - dif types`() {
            val inheritClassName = "Component.AccessibleAWTComponent.AccessibleAWTComponentHandler"
            val inheritFieldName = "accessibleAWTComponentHandler"

            val results = jenkinsSpy.search(inheritClassName)

            verifyOrder {
                jenkinsSpy.search(inheritClassName)
                jenkinsSpy.retrieveClassByUrl("Inherit1", false)
                jenkinsSpy.retrieveClassByUrl("Inherit2", false)
            }

            assertThat(results.size).isEqualTo(2)
            assertThat(results[0].name).isEqualTo(inheritFieldName)
            assertThat(results[1].name).isEqualTo(inheritClassName)
        }

        /**
         * Creates a [ClassInformation] spy object and sets its name and makes [Jenkins] retrieveClassByUrl method
         * return the class with the updated name, if queried for.
         *
         * @param name The new spy's name
         * @param url The fake url to the new spy
         * @param spyMemberList The list of members to return when the searchAll method is called
         */
        private fun createClassSpy(name: String, url: String, spyMemberList: List<Information> = emptyList()) {
            clearMocks(classInfoMock)

            val newClassSpy = spyk(classInfoMock)
            newClassSpy.name = name

            every { jenkinsSpy.retrieveClassByUrl(url, any()) } returns newClassSpy
            every { newClassSpy.searchAll(any()) } returns spyMemberList
        }

        /**
         * Creates a list of members to create a spy object for
         *
         * @param memberPair A [Pair] which contains the member name and type
         * @return A list of all of the created member spies
         */
        private fun createMemberSpyList(vararg memberPair: Pair<String, String>): List<Information> {
            return memberPair.map {
                val spyName = it.first

                val spy = when (it.second.toLowerCase()) {
                    "method" -> spyk(methodMock)
                    "enum" -> spyk(enumMock)
                    "field" -> spyk(fieldMock)
                    else -> spyk(classInfoMock)
                }

                spy.name = spyName
                return@map spy
            }
        }

    }

    @Nested
    inner class RetrieveClassTest {

        private val classInfoMock: ClassInformation = mockk()
        private val classInfoSpy = spyk(classInfoMock)

        @BeforeEach
        fun init() {
            classInfoSpy.name = "String"

            every { jenkinsSpy.classMap } returns mapOf(Pair("StringURL", "String"))
            every { jenkinsSpy.retrieveClassByUrl("StringURL", any()) } returns classInfoSpy
        }

        @Test
        fun `search classes`() {
            val retrievedClass = jenkinsSpy.searchClasses("String")

            verifySequence {
                jenkinsSpy.searchClasses("String")
                jenkinsSpy.classMap
                jenkinsSpy.retrieveClassByUrl("StringURL", false)
            }

            assertThat(retrievedClass.size).isEqualTo(1)
            assertThat(retrievedClass[0].name).isEqualTo("String")
        }

        @Test
        fun `retrieve class by name with limited param`() {
            val retrievedClass = jenkinsSpy.retrieveClass("String", false)

            verifySequence {
                jenkinsSpy.retrieveClass("String", false)
                jenkinsSpy.classMap
                jenkinsSpy.retrieveClassByUrl("StringURL", false)
            }

            assertThat(retrievedClass.name).isEqualTo("String")
        }

        @Test
        fun `retrieve class by name - exception`() {
            assertThatThrownBy {
                jenkinsSpy.retrieveClass("NonExistent")

                verifySequence {
                    jenkinsSpy.retrieveClass("NonExistent")
                    jenkinsSpy.retrieveClass("NonExistent", false)
                }
            }

        }

        @Test
        fun `retrieve class by name without limited param`() {
            val retrievedClass = jenkinsSpy.retrieveClass("String")

            verifySequence {
                jenkinsSpy.retrieveClass("String")
                jenkinsSpy.retrieveClass("String", false)
                jenkinsSpy.classMap
                jenkinsSpy.retrieveClassByUrl("StringURL", false)
            }

            assertThat(retrievedClass.name).isEqualTo("String")
        }


        @Test
        fun `retrieve class by url`() {
            val retrievedClass = jenkinsSpy.retrieveClassByUrl("StringURL", false)
            assertThat(retrievedClass.name).isEqualTo("String")
        }
    }

}