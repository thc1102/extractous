package ai.yobix;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Debug test for GraalVM agent metadata collection
 *
 * This test demonstrates:
 * 1. Parsing a file with embedded documents disabled (parseFileToString)
 * 2. Recursive parsing to extract all embedded documents (parseFileRecursive, parseBytesRecursive)
 * 3. Recursive directory traversal to parse all files in a directory
 *
 * Run with:
 *   Single file: .\gradlew.bat runWithAgentMerge -Pargs="path/to/test/file"
 *   Directory:   .\gradlew.bat runWithAgentMerge -Pargs="path/to/test/directory"
 *
 * Options:
 *   --recursive  : Process all files in directory recursively (default: true)
 *   --max-depth=N: Maximum directory depth to traverse (default: unlimited)
 */
public class DebugTest {

    // Skip hidden files and system files
    private static final Set<String> SKIP_FILE_PREFIXES = new HashSet<>(Arrays.asList(
            ".", "~", "$"  // Skip hidden files, temp files, and system files
    ));

    // Statistics tracking
    private static class Statistics {
        int totalFiles = 0;
        int successFiles = 0;
        int failedFiles = 0;
        int skippedFiles = 0;
        int totalDocuments = 0;
        long totalContentLength = 0;
        long startTime = System.currentTimeMillis();

        void printSummary() {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("\n" + "=".repeat(70));
            System.out.println("PROCESSING SUMMARY");
            System.out.println("=".repeat(70));
            System.out.println("Total files processed: " + totalFiles);
            System.out.println("  ✓ Successful: " + successFiles);
            System.out.println("  ✗ Failed: " + failedFiles);
            System.out.println("  ⊘ Skipped: " + skippedFiles);
            System.out.println("Total documents extracted: " + totalDocuments);
            System.out.println("Total content length: " + totalContentLength + " chars");
            System.out.println("Processing time: " + duration + " ms");
            System.out.println("=".repeat(70));
        }
    }

    public static void main(String[] args) {
        // Parse command line arguments
        boolean recursive = true;
        int maxDepth = Integer.MAX_VALUE;
        String targetPath = null;

        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (arg.equals("--recursive=false") || arg.equals("--no-recursive")) {
                    recursive = false;
                } else if (arg.startsWith("--max-depth=")) {
                    maxDepth = Integer.parseInt(arg.substring("--max-depth=".length()));
                }
            } else {
                targetPath = arg;
            }
        }

        // Default test file path if none provided
        if (targetPath == null) {
            targetPath = "../../../test_files/documents";
        }

        File target = new File(targetPath);
        if (!target.exists()) {
            System.err.println("❌ Error: Path does not exist: " + targetPath);
            System.exit(1);
        }

        System.out.println("=".repeat(70));
        System.out.println("EXTRACTOUS DEBUG TEST");
        System.out.println("=".repeat(70));
        System.out.println("Target path: " + targetPath);
        System.out.println("Path type: " + (target.isDirectory() ? "Directory" : "File"));
        if (target.isDirectory()) {
            System.out.println("Recursive: " + recursive);
            System.out.println("Max depth: " + (maxDepth == Integer.MAX_VALUE ? "unlimited" : maxDepth));
        }
        System.out.println("=".repeat(70));

        // Create parser configurations
        PDFParserConfig pdfConfig = new PDFParserConfig();
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();

        Statistics stats = new Statistics();

        if (target.isFile()) {
            // Process single file
            processSingleFile(target.getAbsolutePath(), pdfConfig, officeConfig, tesseractConfig, stats);
        } else {
            // Process directory
            List<File> files = collectFiles(target, recursive, maxDepth);
            System.out.println("\nFound " + files.size() + " files to process\n");

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                System.out.println("\n[" + (i + 1) + "/" + files.size() + "] Processing: " + file.getName());
                System.out.println("  Path: " + file.getAbsolutePath());
                processSingleFile(file.getAbsolutePath(), pdfConfig, officeConfig, tesseractConfig, stats);
            }
        }

        stats.printSummary();
    }

    /**
     * Collect all files from directory recursively
     */
    private static List<File> collectFiles(File directory, boolean recursive, int maxDepth) {
        List<File> result = new ArrayList<>();
        collectFilesRecursive(directory, result, recursive, 0, maxDepth);
        return result;
    }

    /**
     * Recursive helper to collect files
     */
    private static void collectFilesRecursive(File dir, List<File> result,
                                              boolean recursive, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                if (isSupportedFile(file)) {
                    result.add(file);
                }
            } else if (file.isDirectory() && recursive) {
                collectFilesRecursive(file, result, recursive, currentDepth + 1, maxDepth);
            }
        }
    }

    /**
     * Check if file should be processed (skip hidden and system files)
     */
    private static boolean isSupportedFile(File file) {
        String name = file.getName();

        // Skip hidden files, temp files, and system files
        for (String prefix : SKIP_FILE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return false;
            }
        }

        // Process all other files
        return true;
    }

    /**
     * Process a single file with all test methods
     */
    private static void processSingleFile(String filePath, PDFParserConfig pdfConfig,
                                          OfficeParserConfig officeConfig,
                                          TesseractOCRConfig tesseractConfig,
                                          Statistics stats) {
        stats.totalFiles++;

        try {
            // Main test: parseFileRecursive
            RecursiveResult result = TikaNativeMain.parseFileRecursive(
                    filePath,
                    10000,
                    pdfConfig,
                    officeConfig,
                    tesseractConfig,
                    false
            );

            if (result.isError()) {
                stats.failedFiles++;
                System.out.println("  ❌ Failed: " + result.getErrorMessage());
            } else {
                stats.successFiles++;
                List<Metadata> metadataList = result.getMetadataList();
                stats.totalDocuments += metadataList.size();

                System.out.println("  ✓ Success!");
                System.out.println("    Documents extracted: " + metadataList.size());

                // Calculate total content length
                long contentLength = 0;
                for (Metadata metadata : metadataList) {
                    String content = metadata.get(TikaCoreProperties.TIKA_CONTENT);
                    if (content != null) {
                        contentLength += content.length();
                    }
                }
                stats.totalContentLength += contentLength;
                System.out.println("    Total content length: " + contentLength + " chars");

                // Print document types
                if (metadataList.size() > 1) {
                    System.out.println("    Document types:");
                    for (int i = 0; i < metadataList.size(); i++) {
                        String contentType = metadataList.get(i).get(Metadata.CONTENT_TYPE);
                        System.out.println("      [" + i + "] " + contentType);
                    }
                }
            }
        } catch (Exception e) {
            stats.failedFiles++;
            System.out.println("  ❌ Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test parseFileToString with embedded documents disabled
     */
    private static void testParseFileToString(String filePath, PDFParserConfig pdfConfig,
                                              OfficeParserConfig officeConfig,
                                              TesseractOCRConfig tesseractConfig) {
        System.out.println("\n[TEST 1] parseFileToString (non-recursive)");
        System.out.println("-".repeat(70));

        StringResult result = TikaNativeMain.parseFileToString(
                filePath,
                10000,          // maxLength
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false,          // asXML
                false           // asEmbedded - disables embedded document extraction
        );

        if (result.isError()) {
            System.out.println("❌ Error: status = " + result.getStatus());
            System.err.println("   Error message: " + result.getErrorMessage());
        } else {
            System.out.println("✓ Success! Content length: " + result.getContent().length());
            System.out.println("  Metadata fields: " + result.getMetadata().size());

            // Print first 200 characters
            String content = result.getContent();
            if (content.length() > 200) {
                System.out.println("  Content preview: " + content.substring(0, 200) + "...");
            }
        }
    }

    /**
     * Test parseFileRecursive - extracts container + all embedded documents
     * This is the main test for RecursiveResult metadata collection
     */
    private static void testParseFileRecursive(String filePath, PDFParserConfig pdfConfig,
                                               OfficeParserConfig officeConfig,
                                               TesseractOCRConfig tesseractConfig) {
        System.out.println("\n[TEST 2] parseFileRecursive");
        System.out.println("-".repeat(70));

        RecursiveResult result = TikaNativeMain.parseFileRecursive(
                filePath,
                10000,          // maxLength per document
                pdfConfig,
                officeConfig,
                tesseractConfig,
                false
        );

        // Test all RecursiveResult methods (for metadata collection)
        if (result.isError()) {
            System.out.println("❌ Error: status = " + result.getStatus());
            System.err.println("   Error message: " + result.getErrorMessage());
        } else {
            List<Metadata> metadataList = result.getMetadataList();
            System.out.println("✓ Success!");
            System.out.println("  Total documents: " + metadataList.size());

            // Print info for each document
            for (int i = 0; i < metadataList.size(); i++) {
                Metadata metadata = metadataList.get(i);
                String content = metadata.get(TikaCoreProperties.TIKA_CONTENT);
                String contentType = metadata.get(Metadata.CONTENT_TYPE);

                if (i == 0) {
                    System.out.println("\n  [Container Document]");
                } else {
                    System.out.println("\n  [Embedded Document " + i + "]");
                }

                System.out.println("    Content type: " + contentType);
                System.out.println("    Content length: " + (content != null ? content.length() : 0));
                System.out.println("    Metadata fields: " + metadata.size());

                // Print content preview
                if (content != null && content.length() > 100) {
                    System.out.println("    Content preview: " + content.substring(0, 100) + "...");
                }
            }
        }
    }

    /**
     * Test parseBytesRecursive - also important for metadata collection
     */
    private static void testParseBytesRecursive(String filePath, PDFParserConfig pdfConfig,
                                                OfficeParserConfig officeConfig,
                                                TesseractOCRConfig tesseractConfig) {
        System.out.println("\n[TEST 3] parseBytesRecursive");
        System.out.println("-".repeat(70));

        try {
            // Read file as bytes
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            ByteBuffer buffer = ByteBuffer.wrap(fileBytes);

            System.out.println("  File size: " + fileBytes.length + " bytes");

            RecursiveResult result = TikaNativeMain.parseBytesRecursive(
                    buffer,
                    10000,          // maxLength per document
                    pdfConfig,
                    officeConfig,
                    tesseractConfig,
                    false
            );

            if (result.isError()) {
                System.out.println("❌ Error: status = " + result.getStatus());
                System.err.println("   Error message: " + result.getErrorMessage());
            } else {
                List<Metadata> metadataList = result.getMetadataList();
                System.out.println("✓ Success!");
                System.out.println("  Total documents: " + metadataList.size());

                // Just print summary for this test
                int totalContent = 0;
                for (Metadata metadata : metadataList) {
                    String content = metadata.get(TikaCoreProperties.TIKA_CONTENT);
                    totalContent += (content != null ? content.length() : 0);
                }
                System.out.println("  Total content length: " + totalContent);
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to read file: " + e.getMessage());
        }
    }
}
