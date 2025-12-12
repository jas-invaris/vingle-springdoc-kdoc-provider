package dev.vingle.kdoc.processor

import com.tschuchort.compiletesting.*
import dev.vingle.kdoc.ExampleClassWithDocs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.build.isModuleMappingFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.KFunction

@OptIn(ExperimentalCompilerApi::class)
class GenerateDocumentationFile {
    private val testLanguageVersion = "2.2"

    @Test
    fun callWithEmptyArguments() {
        val compilation = KotlinCompilation().apply {
            configureKsp {
                languageVersion = testLanguageVersion
                sources = emptyList()
            }
            symbolProcessorProviders = mutableListOf(KDocProcessorProvider())
        }
        val result = compilation.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertIterableEquals(emptySet<Nothing>(), result.generatedFiles.filter { !it.isModuleMappingFile() })
        assertIterableEquals(emptySet<Nothing>(), result.generatedStubFiles)
        assertIterableEquals(emptySet<Nothing>(), result.sourcesGeneratedBySymbolProcessor.toList())
    }

    @Test
    fun generateDocumentation() {
        val compilation = KotlinCompilation().apply {
            configureKsp {
                languageVersion = testLanguageVersion
                sources = listOf(getExampleClassSourceFile())
            }
            symbolProcessorProviders = mutableListOf(KDocProcessorProvider())
            kspProcessorOptions = mutableMapOf("kdoc.all-files" to true.toString(), "kdoc.debug" to true.toString())
        }
        val result = compilation.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertIterableEquals(emptySet<Nothing>(), result.generatedStubFiles)

        val filteredSources = result.generatedFiles.filter { !it.isModuleMappingFile() }.toList()
        assertEquals(1, filteredSources.size)
        assertEquals("${ExampleClassWithDocs::class.simpleName}.class", filteredSources.firstOrNull()?.name)

        val generatedJsonFiles = result.sourcesGeneratedBySymbolProcessor.toList()
        assertEquals(1, generatedJsonFiles.size)
        assertEquals("${ExampleClassWithDocs::class.simpleName}.json", generatedJsonFiles.firstOrNull()?.name)
        val generatedJsonFile = generatedJsonFiles.first()
        checkGeneratedDocumentationFile(generatedJsonFile)
    }

    private fun checkGeneratedDocumentationFile(generatedJsonFile: File) {
        // Parse and check the resulting JSON file.
        val json = Json.parseToJsonElement(generatedJsonFile.readText()).jsonObject
        assertEquals(ExampleClassWithDocs::class.qualifiedName, json["name"]?.jsonPrimitive?.content)
        // Class comment should match.
        val generatedClassComment = json["comment"]?.jsonObject?.get("text")?.jsonPrimitive?.content
        assertEquals(true, generatedClassComment?.startsWith("This is a data class"))
        assertEquals(true, generatedClassComment?.endsWith("be used by the tests."))

        // Methods should exist in the generated JSON with the respective parameter types and comments.
        val expectedMethods = ExampleClassWithDocs::class.java.declaredMethods.asSequence().filter {
            sequenceOf(
                ExampleClassWithDocs::methodWithoutParameters, ExampleClassWithDocs::methodWithParameters,
            ).map(KFunction<*>::name).contains(it.name)
        }.toSet()
        val generatedMethods = json["methods"]?.jsonArray?.map { it.jsonObject }
        assertNotNull(generatedMethods, "Did not find any methods in the output.\n$json")
        for (expectedMethod in expectedMethods) {
            val matchingMethod = generatedMethods?.singleOrNull { generated ->
                expectedMethod.name == generated["name"]?.jsonPrimitive?.content
            }
            assertNotNull(matchingMethod, "Expected method '${expectedMethod.name}' to exist in the output.\n$json")
            assertIterableEquals(
                expectedMethod.parameters.map { it.type.kotlin.simpleName },
                matchingMethod!!["paramTypes"]?.jsonArray?.map { it.jsonPrimitive.content },
            )
            assertEquals(
                false, matchingMethod["comment"]?.jsonObject["text"]?.jsonPrimitive?.content?.isBlank(),
                "Comment was blank or nonexistent for method '${expectedMethod.name}' in the output.\n$matchingMethod",
            )
        }

        // Fields should exist in the generated JSON.
        val expectedFields = ExampleClassWithDocs::class.java.declaredFields.toSet()
        val generatedFields = json["fields"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
        for (expectedField in expectedFields) {
            val matchingField = generatedFields.singleOrNull { generated ->
                generated["name"]?.jsonPrimitive?.content == expectedField.name
            }
            assertNotNull(matchingField, "Expected field '${expectedField.name}' to exist in the output.")
            assertEquals(false, matchingField!!["comment"]?.jsonObject["text"]?.jsonPrimitive?.content?.isBlank())
        }
    }

    @Test
    fun `only processes RestControllers by default`() {
        TODO()
    }

    private fun getExampleClassSourceFile(): SourceFile {
        val classFilePath = "../kdoc-runtime/src/testFixtures/kotlin/${
            ExampleClassWithDocs::class.java.canonicalName!!.replace('.', '/')
        }.kt"
        val sourceCode = File(classFilePath).readText()
        return SourceFile.kotlin(ExampleClassWithDocs::class.simpleName!! + ".kt", sourceCode)
    }
}
