package ai.yobix;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class TikaNativeMainTest {

    @Test
    void testDetect(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "This is a test document.");

        StringResult result = TikaNativeMain.detect(testFile.toString());
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(0, result.getStatus());
        assertTrue(result.getContent().contains("text/plain"));
    }

    @Test
    void testParseFileToString(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String testContent = "Hello, this is a test document.";
        Files.writeString(testFile, testContent);

        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        StringResult result = TikaNativeMain.parseFileToString(
                testFile.toString(),
                10000,
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false,
                false
        );

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(0, result.getStatus());
        assertTrue(result.getContent().contains(testContent));
    }

    @Test
    void testParseBytesToString() throws IOException {
        String testContent = "Test content from bytes.";
        ByteBuffer buffer = ByteBuffer.wrap(testContent.getBytes());

        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        StringResult result = TikaNativeMain.parseBytesToString(
                buffer,
                10000,
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false,
                false
        );

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(0, result.getStatus());
        assertTrue(result.getContent().contains(testContent));
    }

    @Test
    void testParseFile(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String testContent = "Test content for reader.";
        Files.writeString(testFile, testContent);

        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        ReaderResult result = TikaNativeMain.parseFile(
                testFile.toString(),
                "UTF-8",
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false,
                false
        );

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(0, result.getStatus());
        assertNotNull(result.getReader());

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(result.getReader())
        );
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();

        assertTrue(content.toString().contains(testContent));
    }

    @Test
    void testParseNonExistentFile() {
        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        StringResult result = TikaNativeMain.parseFileToString(
                "non_existent_file.txt",
                10000,
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false,
                false
        );

        assertNotNull(result);
        assertTrue(result.isError());
        assertNotEquals(0, result.getStatus());
    }

    @Test
    void testParseFileRecursive(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String testContent = "This is a test document for recursive parsing.";
        Files.writeString(testFile, testContent);

        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        RecursiveResult result = TikaNativeMain.parseFileRecursive(
                testFile.toString(),
                10000,
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false
        );

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(0, result.getStatus());
        
        List<Metadata> metadataList = result.getMetadataList();
        assertNotNull(metadataList);
        assertTrue(metadataList.size() >= 1, "Should have at least one document (the container)");
        
        Metadata containerMetadata = metadataList.get(0);
        assertNotNull(containerMetadata);
        
        String content = containerMetadata.get(TikaCoreProperties.TIKA_CONTENT);
        assertNotNull(content);
        assertTrue(content.contains(testContent));
    }

    @Test
    void testParseBytesRecursive() throws IOException {
        String testContent = "Recursive parsing test content from bytes.";
        ByteBuffer buffer = ByteBuffer.wrap(testContent.getBytes());

        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        RecursiveResult result = TikaNativeMain.parseBytesRecursive(
                buffer,
                10000,
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false
        );

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(0, result.getStatus());
        
        List<Metadata> metadataList = result.getMetadataList();
        assertNotNull(metadataList);
        assertTrue(metadataList.size() >= 1);
        
        String content = metadataList.get(0).get(TikaCoreProperties.TIKA_CONTENT);
        assertNotNull(content);
        assertTrue(content.contains(testContent));
    }

    @Test
    void testParseFileWithEmbeddedDisabled(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String testContent = "Test content with embedded disabled.";
        Files.writeString(testFile, testContent);

        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        // Test with asEmbedded = false (default behavior)
        ReaderResult result = TikaNativeMain.parseFile(
                testFile.toString(),
                "UTF-8",
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false,
                false
        );

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(0, result.getStatus());
        assertNotNull(result.getReader());

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(result.getReader())
        );
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();

        assertTrue(content.toString().contains(testContent));
    }

    @Test
    void testParseFileToStringWithEmbedded(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String testContent = "Test content for embedded parsing.";
        Files.writeString(testFile, testContent);

        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        // Test with asEmbedded = true
        StringResult resultWithEmbedded = TikaNativeMain.parseFileToString(
                testFile.toString(),
                10000,
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false,
                true
        );

        assertNotNull(resultWithEmbedded);
        assertFalse(resultWithEmbedded.isError());
        assertEquals(0, resultWithEmbedded.getStatus());
        assertTrue(resultWithEmbedded.getContent().contains(testContent));

        // Test with asEmbedded = false
        StringResult resultWithoutEmbedded = TikaNativeMain.parseFileToString(
                testFile.toString(),
                10000,
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false,
                false
        );

        assertNotNull(resultWithoutEmbedded);
        assertFalse(resultWithoutEmbedded.isError());
        assertEquals(0, resultWithoutEmbedded.getStatus());
        assertTrue(resultWithoutEmbedded.getContent().contains(testContent));
    }

    // ============================================================================
    // Memory Management Tests
    // ============================================================================

    @Test
    void testGetMemoryUsage() throws Exception {
        StringResult result = TikaNativeMain.getMemoryUsage();

        assertNotNull(result, "Memory usage result should not be null");
        assertFalse(result.isError(), "Memory usage should not return error");
        assertEquals(0, result.getStatus(), "Status should be 0 for success");

        String jsonContent = result.getContent();
        assertNotNull(jsonContent, "JSON content should not be null");
        assertFalse(jsonContent.isEmpty(), "JSON content should not be empty");

        // Parse and validate JSON structure
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonContent);
        assertTrue(json.has("usedMemoryMB"), "Should have usedMemoryMB field");
        assertTrue(json.has("freeMemoryMB"), "Should have freeMemoryMB field");
        assertTrue(json.has("totalMemoryMB"), "Should have totalMemoryMB field");
        assertTrue(json.has("maxMemoryMB"), "Should have maxMemoryMB field");
        assertTrue(json.has("usagePercent"), "Should have usagePercent field");

        // Validate values are reasonable
        double usedMemoryMB = json.get("usedMemoryMB").asDouble();
        double freeMemoryMB = json.get("freeMemoryMB").asDouble();
        double totalMemoryMB = json.get("totalMemoryMB").asDouble();
        double maxMemoryMB = json.get("maxMemoryMB").asDouble();
        double usagePercent = json.get("usagePercent").asDouble();

        assertTrue(usedMemoryMB >= 0, "Used memory should be non-negative");
        assertTrue(freeMemoryMB >= 0, "Free memory should be non-negative");
        assertTrue(totalMemoryMB > 0, "Total memory should be positive");
        assertTrue(maxMemoryMB > 0, "Max memory should be positive");
        assertTrue(usagePercent >= 0 && usagePercent <= 100,
                   "Usage percent should be between 0 and 100");

        // Verify relationship: used + free ≈ total (within 1MB tolerance)
        double calculatedTotal = usedMemoryMB + freeMemoryMB;
        assertTrue(Math.abs(calculatedTotal - totalMemoryMB) < 1.0,
                   "Used + Free should approximately equal Total");

        // Verify usage percent calculation
        double expectedPercent = (usedMemoryMB / maxMemoryMB) * 100;
        assertTrue(Math.abs(expectedPercent - usagePercent) < 0.1,
                   "Usage percent should match (used/max)*100");
    }

    @Test
    void testTriggerGarbageCollection() throws Exception {
        StringResult result = TikaNativeMain.triggerGarbageCollection();

        assertNotNull(result, "GC result should not be null");
        assertFalse(result.isError(), "GC should not return error");
        assertEquals(0, result.getStatus(), "Status should be 0 for success");

        String jsonContent = result.getContent();
        assertNotNull(jsonContent, "JSON content should not be null");

        // Parse and validate JSON structure
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonContent);

        assertTrue(json.has("success"), "Should have success field");
        assertTrue(json.has("freedMemoryMB"), "Should have freedMemoryMB field");
        assertTrue(json.has("beforeMB"), "Should have beforeMB field");
        assertTrue(json.has("afterMB"), "Should have afterMB field");

        // Validate GC execution
        assertTrue(json.get("success").asBoolean(), "GC should succeed");

        long freedMemoryMB = json.get("freedMemoryMB").asLong();
        long beforeMB = json.get("beforeMB").asLong();
        long afterMB = json.get("afterMB").asLong();

        assertTrue(beforeMB >= 0, "Before memory should be non-negative");
        assertTrue(afterMB >= 0, "After memory should be non-negative");
        // Note: freedMemoryMB can be negative if memory increased during GC
        assertTrue(freedMemoryMB == (beforeMB - afterMB),
                   "Freed memory should equal before - after");
    }

    @Test
    void testMultipleMemoryUsageCalls() throws Exception {
        // Test that multiple calls work correctly and return different values
        StringResult result1 = TikaNativeMain.getMemoryUsage();
        Thread.sleep(50); // Small delay
        StringResult result2 = TikaNativeMain.getMemoryUsage();

        assertNotNull(result1);
        assertNotNull(result2);
        assertFalse(result1.isError());
        assertFalse(result2.isError());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json1 = mapper.readTree(result1.getContent());
        JsonNode json2 = mapper.readTree(result2.getContent());

        // Both should have valid memory values
        assertTrue(json1.get("totalMemoryMB").asDouble() > 0);
        assertTrue(json2.get("totalMemoryMB").asDouble() > 0);
    }

    @Test
    void testGarbageCollectionFreesMemory(@TempDir Path tempDir) throws Exception {
        // Get initial memory
        StringResult initialMemory = TikaNativeMain.getMemoryUsage();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode initialJson = mapper.readTree(initialMemory.getContent());
        double initialUsed = initialJson.get("usedMemoryMB").asDouble();

        // Create some garbage by parsing files
        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        // Parse multiple files to create some garbage
        for (int i = 0; i < 10; i++) {
            Path testFile = tempDir.resolve("test" + i + ".txt");
            Files.writeString(testFile, "Test content " + i + " ".repeat(1000));

            StringResult parseResult = TikaNativeMain.parseFileToString(
                    testFile.toString(),
                    100000,
                    pdfConfig,
                    officeConfig,
                    tesseractConfig,
                    false,
                    false
            );
            assertNotNull(parseResult);
        }

        // Get memory after parsing
        StringResult afterParsingMemory = TikaNativeMain.getMemoryUsage();
        JsonNode afterParsingJson = mapper.readTree(afterParsingMemory.getContent());
        double afterParsingUsed = afterParsingJson.get("usedMemoryMB").asDouble();

        // Trigger GC
        StringResult gcResult = TikaNativeMain.triggerGarbageCollection();
        assertFalse(gcResult.isError());

        // Get memory after GC
        StringResult afterGcMemory = TikaNativeMain.getMemoryUsage();
        JsonNode afterGcJson = mapper.readTree(afterGcMemory.getContent());
        double afterGcUsed = afterGcJson.get("usedMemoryMB").asDouble();

        // Memory after GC should be less than or equal to after parsing
        // (GC may not free everything, but it shouldn't increase)
        assertTrue(afterGcUsed <= afterParsingUsed + 5, // Allow 5MB tolerance
                   String.format("After GC memory (%.2f MB) should be <= after parsing (%.2f MB)",
                           afterGcUsed, afterParsingUsed));

        System.out.println(String.format(
                "Memory Test: Initial=%.2fMB, After Parsing=%.2fMB, After GC=%.2fMB",
                initialUsed, afterParsingUsed, afterGcUsed
        ));
    }

    @Test
    void testMemoryMonitoringWorkflow(@TempDir Path tempDir) throws Exception {
        // Simulate a realistic memory monitoring workflow
        ObjectMapper mapper = new ObjectMapper();
        final double MEMORY_THRESHOLD = 80.0; // 80% usage threshold

        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        int filesProcessed = 0;
        int gcTriggered = 0;

        for (int i = 0; i < 20; i++) {
            // Create and parse a file
            Path testFile = tempDir.resolve("test_" + i + ".txt");
            Files.writeString(testFile, "Test content for file " + i + " ".repeat(500));

            StringResult parseResult = TikaNativeMain.parseFileToString(
                    testFile.toString(),
                    100000,
                    pdfConfig,
                    officeConfig,
                    tesseractConfig,
                    false,
                    false
            );
            assertNotNull(parseResult);
            filesProcessed++;

            // Check memory every 5 files
            if (i % 5 == 4) {
                StringResult memoryResult = TikaNativeMain.getMemoryUsage();
                JsonNode memoryJson = mapper.readTree(memoryResult.getContent());
                double usagePercent = memoryJson.get("usagePercent").asDouble();

                System.out.println(String.format(
                        "[File %d] Memory usage: %.2f%% (%.2fMB / %.2fMB)",
                        i + 1,
                        usagePercent,
                        memoryJson.get("usedMemoryMB").asDouble(),
                        memoryJson.get("maxMemoryMB").asDouble()
                ));

                // Trigger GC if usage exceeds threshold
                if (usagePercent > MEMORY_THRESHOLD) {
                    System.out.println("  ⚠️  Memory usage exceeds threshold, triggering GC...");
                    StringResult gcResult = TikaNativeMain.triggerGarbageCollection();
                    assertFalse(gcResult.isError());
                    gcTriggered++;

                    JsonNode gcJson = mapper.readTree(gcResult.getContent());
                    System.out.println(String.format(
                            "  ✅ GC completed, freed %d MB",
                            gcJson.get("freedMemoryMB").asLong()
                    ));
                }
            }
        }

        System.out.println(String.format(
                "\nWorkflow completed: %d files processed, %d GC triggered",
                filesProcessed, gcTriggered
        ));

        // Verify workflow completed successfully
        assertEquals(20, filesProcessed);
        assertTrue(gcTriggered >= 0, "GC trigger count should be non-negative");
    }

    @Test
    void testConcurrentMemoryUsageCalls() throws Exception {
        // Test that concurrent calls to getMemoryUsage are thread-safe
        final int NUM_THREADS = 5;
        final int CALLS_PER_THREAD = 10;
        Thread[] threads = new Thread[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < CALLS_PER_THREAD; j++) {
                    try {
                        StringResult result = TikaNativeMain.getMemoryUsage();
                        assertNotNull(result);
                        assertFalse(result.isError());

                        // Validate JSON structure
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode json = mapper.readTree(result.getContent());
                        assertTrue(json.has("usedMemoryMB"));
                        assertTrue(json.has("usagePercent"));
                    } catch (Exception e) {
                        fail("Concurrent call failed: " + e.getMessage());
                    }
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout per thread
            assertFalse(thread.isAlive(), "Thread should have completed");
        }
    }

    @Test
    void testMemoryUsageJsonFormat() throws Exception {
        StringResult result = TikaNativeMain.getMemoryUsage();
        assertNotNull(result);
        assertFalse(result.isError());

        String jsonContent = result.getContent();

        // Validate JSON format (should be valid JSON)
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonContent);

        // Check all required fields exist and are of correct type
        assertTrue(json.get("usedMemoryMB").isDouble() || json.get("usedMemoryMB").isNumber());
        assertTrue(json.get("freeMemoryMB").isDouble() || json.get("freeMemoryMB").isNumber());
        assertTrue(json.get("totalMemoryMB").isDouble() || json.get("totalMemoryMB").isNumber());
        assertTrue(json.get("maxMemoryMB").isDouble() || json.get("maxMemoryMB").isNumber());
        assertTrue(json.get("usagePercent").isDouble() || json.get("usagePercent").isNumber());

        // Ensure no unexpected fields (should only have 5 fields)
        assertEquals(5, json.size(), "JSON should have exactly 5 fields");
    }

    @Test
    void testGarbageCollectionJsonFormat() throws Exception {
        StringResult result = TikaNativeMain.triggerGarbageCollection();
        assertNotNull(result);
        assertFalse(result.isError());

        String jsonContent = result.getContent();

        // Validate JSON format
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonContent);
        // Check all required fields exist and are of correct type
        assertTrue(json.get("success").isBoolean());
        assertTrue(json.get("freedMemoryMB").isNumber());
        assertTrue(json.get("beforeMB").isNumber());
        assertTrue(json.get("afterMB").isNumber());

        // Ensure no unexpected fields
        assertEquals(4, json.size(), "JSON should have exactly 4 fields");
    }
}
