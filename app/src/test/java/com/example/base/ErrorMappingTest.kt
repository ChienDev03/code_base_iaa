package com.example.base
import com.example.base.core.network.backendMessage
import org.junit.Assert.*
import org.junit.Test
/** Backend error precedence without exception leakage. */
class ErrorMappingTest {
    @Test fun detailsPrecedeTopLevelMessage() {
        assertEquals("First\nSecond", backendMessage("""{"message":"Fallback","details":[{"message":"First"},{"message":"Second"}]}"""))
    }
    @Test fun malformedOrBlankBodyRequiresResourceFallback() {
        assertNull(backendMessage("internal exception"))
        assertNull(backendMessage("""{"message":" "}"""))
    }
}
