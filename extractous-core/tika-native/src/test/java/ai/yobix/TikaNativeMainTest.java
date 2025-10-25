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
}
