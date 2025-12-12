package dev.vingle.kdoc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RuntimeKDocTest {
    @Test
    fun clearCache() {
        assertEquals(0, RuntimeKDoc.currentCacheSize)
        val classKDoc = RuntimeKDoc.getKDoc(ExampleClassWithDocs::class)
        assertFalse(classKDoc.isEmpty())
        assertEquals(1, RuntimeKDoc.currentCacheSize)
        RuntimeKDoc.clearCache()
        assertEquals(0, RuntimeKDoc.currentCacheSize)
    }

    @Test
    fun getClassKDocNoFileAvailable() {
        var classKDoc = RuntimeKDoc.getKDoc(TestClassWithoutFile::class)
        assertTrue(classKDoc.isEmpty())
        classKDoc = RuntimeKDoc.getKDoc(CorruptedJson::class)
        assertTrue(classKDoc.isEmpty())
    }
}

/** This class does not have a corresponding JSON file on the classpath.  */
private class TestClassWithoutFile

/** This class has a JSON file, but it cannot be deserialized to a class instance. */
private class CorruptedJson
