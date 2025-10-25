package ai.yobix;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.RecursiveParserWrapperHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TikaNativeMain {

    private static final Tika tika = new Tika();

    /**
     * Parses the given file and returns its type as a mime type
     *
     * @param filePath: the path of the file to be parsed
     * @return StringResult
     */
    public static StringResult detect(String filePath) {
        final Path path = Paths.get(filePath);
        final Metadata metadata = new Metadata();

        try (final InputStream stream = TikaInputStream.get(path, metadata)) {
            final String result = tika.detect(stream, metadata);
            return new StringResult(result, metadata);

        } catch (java.io.IOException e) {
            return new StringResult((byte) 1, e.getMessage());
        }
    }

    /**
     * Parses the given file and returns its content as String.
     * To avoid unpredictable excess memory use, the returned string contains only up to maxLength
     * first characters extracted from the input document.
     *
     * @param filePath:  the path of the file to be parsed
     * @param maxLength: maximum length of the returned string
     * @return StringResult
     */
    public static StringResult parseFileToString(
            String filePath,
            int maxLength,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXML,
            boolean asEmbedded
            // maybe replace with a single config class
    ) {
        try {
            final Path path = Paths.get(filePath);
            final Metadata metadata = new Metadata();
            final InputStream stream = TikaInputStream.get(path, metadata);

            String result = parseToStringWithConfig(
                    stream, metadata, maxLength, pdfConfig, officeConfig, tesseractConfig, asXML, asEmbedded);
            // No need to close the stream because parseToString does so
            return new StringResult(result, metadata);
        } catch (java.io.IOException e) {
            return new StringResult((byte) 1, "Could not open file: " + e.getMessage());
        } catch (TikaException e) {
            return new StringResult((byte) 2, "Parse error occurred : " + e.getMessage());
        }
    }

    /**
     * Parses the given Url and returns its content as String
     *
     * @param urlString the url to be parsed
     * @return StringResult
     */
    public static StringResult parseUrlToString(
            String urlString,
            int maxLength,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXML,
            boolean asEmbedded
    ) {
        try {
            final URL url = new URI(urlString).toURL();
            final Metadata metadata = new Metadata();
            final TikaInputStream stream = TikaInputStream.get(url, metadata);

            String result = parseToStringWithConfig(
                    stream, metadata, maxLength, pdfConfig, officeConfig, tesseractConfig, asXML, asEmbedded);
            // No need to close the stream because parseToString does so
            return new StringResult(result, metadata);

        } catch (MalformedURLException e) {
            return new StringResult((byte) 2, "Malformed URL error occurred " + e.getMessage());
        } catch (URISyntaxException e) {
            return new StringResult((byte) 2, "Malformed URI error occurred: " + e.getMessage());
        } catch (java.io.IOException e) {
            return new StringResult((byte) 1, "IO error occurred: " + e.getMessage());
        } catch (TikaException e) {
            return new StringResult((byte) 2, "Parse error occurred : " + e.getMessage());
        }
    }

    /**
     * Parses the given array of bytes and return its content as String.
     *
     * @param data an array of bytes
     * @return StringResult
     */
    public static StringResult parseBytesToString(
            ByteBuffer data,
            int maxLength,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXML,
            boolean asEmbedded
    ) {
        final Metadata metadata = new Metadata();
        final ByteBufferInputStream inStream = new ByteBufferInputStream(data);
        final TikaInputStream stream = TikaInputStream.get(inStream, new TemporaryResources(), metadata);

        try {
            String result = parseToStringWithConfig(
                    stream, metadata, maxLength, pdfConfig, officeConfig, tesseractConfig, asXML, asEmbedded);
            // No need to close the stream because parseToString does so
            return new StringResult(result, metadata);
        } catch (java.io.IOException e) {
            return new StringResult((byte) 1, "IO error occurred: " + e.getMessage());
        } catch (TikaException e) {
            return new StringResult((byte) 2, "Parse error occurred : " + e.getMessage());
        }
    }

    private static String parseToStringWithConfig(
            InputStream stream,
            Metadata metadata,
            int maxLength,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXML,
            boolean asEmbedded
    ) throws IOException, TikaException {
        ContentHandler handler;
        ContentHandler handlerForParser;
        if (asXML) {
            handler = new WriteOutContentHandler(new ToXMLContentHandler(), maxLength);
            handlerForParser = handler;
        } else {
            handler = new WriteOutContentHandler(maxLength);
            handlerForParser = new BodyContentHandler(handler);
        }

        try (stream) {
            final TikaConfig config = TikaConfig.getDefaultConfig();
            final ParseContext parsecontext = new ParseContext();
            final Parser parser = new AutoDetectParser(config);

            parsecontext.set(Parser.class, parser);
            parsecontext.set(PDFParserConfig.class, pdfConfig);
            parsecontext.set(OfficeParserConfig.class, officeConfig);
            parsecontext.set(TesseractOCRConfig.class, tesseractConfig);

            // Disable embedded document parsing if asEmbedded is false
            if (!asEmbedded) {
                parsecontext.set(Parser.class, EmptyParser.INSTANCE);
            }

            parser.parse(stream, handlerForParser, metadata, parsecontext);
        } catch (SAXException e) {
            if (!WriteLimitReachedException.isWriteLimitReached(e)) {
                // This should never happen with BodyContentHandler...
                throw new TikaException("Unexpected SAX processing failure", e);
            }
        }
        return handler.toString();
    }


    /**
     * Parses the given file and returns its content as Reader. The reader can be used
     * to read chunks and must be closed when reading is finished
     *
     * @param filePath the path of the file
     * @param charsetName character encoding
     * @param pdfConfig PDF parser configuration
     * @param officeConfig Office parser configuration
     * @param tesseractConfig OCR configuration
     * @param asXML whether to output as XML
     * @param asEmbedded whether to parse embedded documents (default: false)
     * @return ReaderResult
     */
    public static ReaderResult parseFile(
            String filePath,
            String charsetName,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXML,
            boolean asEmbedded
    ) {
        try {
//            System.out.println("pdfConfig.isExtractInlineImages = " + pdfConfig.isExtractInlineImages());
//            System.out.println("pdfConfig.isExtractMarkedContent = " + pdfConfig.isExtractMarkedContent());
//            System.out.println("pdfConfig.getOcrStrategy = " + pdfConfig.getOcrStrategy());
//            System.out.println("officeConfig.isIncludeHeadersAndFooters = " + officeConfig.isIncludeHeadersAndFooters());
//            System.out.println("officeConfig.isIncludeShapeBasedContent = " + officeConfig.isIncludeShapeBasedContent());
//            System.out.println("ocrConfig.getTimeoutSeconds = " + tesseractConfig.getTimeoutSeconds());
//            System.out.println("ocrConfig.language = " + tesseractConfig.getLanguage());

            final Path path = Paths.get(filePath);
            final Metadata metadata = new Metadata();
            final TikaInputStream stream = TikaInputStream.get(path, metadata);

            return parse(stream, metadata, charsetName, pdfConfig, officeConfig, tesseractConfig, asXML, asEmbedded);

        } catch (java.io.IOException e) {
            return new ReaderResult((byte) 1, "Could not open file: " + e.getMessage());
        }
    }

    /**
     * Parses the given Url and returns its content as Reader. The reader can be used
     * to read chunks and must be closed when reading is finished
     *
     * @param urlString the url to be parsed
     * @param charsetName character encoding
     * @param pdfConfig PDF parser configuration
     * @param officeConfig Office parser configuration
     * @param tesseractConfig OCR configuration
     * @param asXML whether to output as XML
     * @param asEmbedded whether to parse embedded documents (default: false)
     * @return ReaderResult
     */
    public static ReaderResult parseUrl(
            String urlString,
            String charsetName,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXML,
            boolean asEmbedded
    ) {
        try {
            final URL url = new URI(urlString).toURL();
            final Metadata metadata = new Metadata();
            final TikaInputStream stream = TikaInputStream.get(url, metadata);

            return parse(stream, metadata, charsetName, pdfConfig, officeConfig, tesseractConfig, asXML, asEmbedded);

        } catch (MalformedURLException e) {
            return new ReaderResult((byte) 2, "Malformed URL error occurred " + e.getMessage());
        } catch (URISyntaxException e) {
            return new ReaderResult((byte) 3, "Malformed URI error occurred: " + e.getMessage());
        } catch (java.io.IOException e) {
            return new ReaderResult((byte) 1, "IO error occurred: " + e.getMessage());
        }
    }

    /**
     * Parses the given array of bytes and return its content as Reader. The reader can be used
     * to read chunks and must be closed when reading is finished
     *
     * @param data an array of bytes
     * @param charsetName character encoding
     * @param pdfConfig PDF parser configuration
     * @param officeConfig Office parser configuration
     * @param tesseractConfig OCR configuration
     * @param asXML whether to output as XML
     * @param asEmbedded whether to parse embedded documents (default: false)
     * @return ReaderResult
     */
    public static ReaderResult parseBytes(
            ByteBuffer data,
            String charsetName,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXML,
            boolean asEmbedded
    ) {


        final Metadata metadata = new Metadata();
        final ByteBufferInputStream inStream = new ByteBufferInputStream(data);
        final TikaInputStream stream = TikaInputStream.get(inStream, new TemporaryResources(), metadata);

        return parse(stream, metadata, charsetName, pdfConfig, officeConfig, tesseractConfig, asXML, asEmbedded);
    }

    private static ReaderResult parse(
            TikaInputStream inputStream,
            Metadata metadata,
            String charsetName,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXML,
            boolean asEmbedded
    ) {
        try {

            final TikaConfig config = TikaConfig.getDefaultConfig();
            final ParseContext parsecontext = new ParseContext();
            final Parser parser = new AutoDetectParser(config);
            Charset charset;
            try {
                charset = Charset.forName(charsetName);
            } catch (Exception e) {
                charset = StandardCharsets.UTF_8;
            }

            parsecontext.set(Parser.class, parser);
            parsecontext.set(PDFParserConfig.class, pdfConfig);
            parsecontext.set(OfficeParserConfig.class, officeConfig);
            parsecontext.set(TesseractOCRConfig.class, tesseractConfig);

            // Disable embedded document parsing if asEmbedded is false
            if (!asEmbedded) {
                parsecontext.set(Parser.class, EmptyParser.INSTANCE);
            }

            //final Reader reader = new org.apache.tika.parser.ParsingReader(parser, inputStream, metadata, parsecontext);
            final Reader reader = new ParsingReader(parser, inputStream, metadata, parsecontext, asXML, charset.name());

            // Convert Reader which works with chars to ReaderInputStream which works with bytes
            ReaderInputStream readerInputStream = ReaderInputStream.builder()
                    .setReader(reader)
                    .setCharset(charset)
                    .get();

            return new ReaderResult(readerInputStream, metadata);

        } catch (java.io.IOException e) {
            return new ReaderResult((byte) 1, "IO error occurred: " + e.getMessage());
        }

    }

    /**
     * Parses the given file recursively, including all embedded documents.
     * Returns a list of metadata for the container document and all embedded documents.
     *
     * @param filePath the path of the file to be parsed
     * @param maxLength maximum length of content for each document
     * @param pdfConfig PDF parser configuration
     * @param officeConfig Office parser configuration
     * @param tesseractConfig OCR configuration
     * @return RecursiveResult containing list of Metadata for all documents
     */
    public static RecursiveResult parseFileRecursive(
            String filePath,
            int maxLength,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXml
    ) {
        try {
            final Path path = Paths.get(filePath);
            final Metadata metadata = new Metadata();
            final TikaInputStream stream = TikaInputStream.get(path, metadata);

            return parseRecursive(stream, maxLength, pdfConfig, officeConfig, tesseractConfig, asXml);

        } catch (java.io.IOException e) {
            return new RecursiveResult((byte) 1, "Could not open file: " + e.getMessage());
        } catch (TikaException e) {
            return new RecursiveResult((byte) 2, "Parse error occurred: " + e.getMessage());
        } catch (SAXException e) {
            return new RecursiveResult((byte) 2, "SAX error occurred: " + e.getMessage());
        }
    }

    /**
     * Parses the given URL recursively, including all embedded documents.
     * Returns a list of metadata for the container document and all embedded documents.
     *
     * @param urlString the URL to be parsed
     * @param maxLength maximum length of content for each document
     * @param pdfConfig PDF parser configuration
     * @param officeConfig Office parser configuration
     * @param tesseractConfig OCR configuration
     * @return RecursiveResult containing list of Metadata for all documents
     */
    public static RecursiveResult parseUrlRecursive(
            String urlString,
            int maxLength,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXml
    ) {
        try {
            final URL url = new URI(urlString).toURL();
            final Metadata metadata = new Metadata();
            final TikaInputStream stream = TikaInputStream.get(url, metadata);

            return parseRecursive(stream, maxLength, pdfConfig, officeConfig, tesseractConfig, asXml);

        } catch (MalformedURLException e) {
            return new RecursiveResult((byte) 2, "Malformed URL error occurred: " + e.getMessage());
        } catch (URISyntaxException e) {
            return new RecursiveResult((byte) 2, "Malformed URI error occurred: " + e.getMessage());
        } catch (java.io.IOException e) {
            return new RecursiveResult((byte) 1, "IO error occurred: " + e.getMessage());
        } catch (TikaException e) {
            return new RecursiveResult((byte) 2, "Parse error occurred: " + e.getMessage());
        } catch (SAXException e) {
            return new RecursiveResult((byte) 2, "SAX error occurred: " + e.getMessage());
        }
    }

    /**
     * Parses the given bytes recursively, including all embedded documents.
     * Returns a list of metadata for the container document and all embedded documents.
     *
     * @param data byte buffer containing the document data
     * @param maxLength maximum length of content for each document
     * @param pdfConfig PDF parser configuration
     * @param officeConfig Office parser configuration
     * @param tesseractConfig OCR configuration
     * @return RecursiveResult containing list of Metadata for all documents
     */
    public static RecursiveResult parseBytesRecursive(
            ByteBuffer data,
            int maxLength,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXml
    ) {
        try {
            final Metadata metadata = new Metadata();
            final ByteBufferInputStream inStream = new ByteBufferInputStream(data);
            final TikaInputStream stream = TikaInputStream.get(inStream, new TemporaryResources(), metadata);

            return parseRecursive(stream, maxLength, pdfConfig, officeConfig, tesseractConfig, asXml);

        } catch (java.io.IOException e) {
            return new RecursiveResult((byte) 1, "IO error occurred: " + e.getMessage());
        } catch (TikaException e) {
            return new RecursiveResult((byte) 2, "Parse error occurred: " + e.getMessage());
        } catch (SAXException e) {
            return new RecursiveResult((byte) 2, "SAX error occurred: " + e.getMessage());
        }
    }

    /**
     * Internal method to parse documents recursively using RecursiveParserWrapper.
     * Extracts content and metadata from both the container document and all embedded documents.
     * 
     * FAULT TOLERANCE: Uses RecursiveParserWrapper with catchEmbeddedExceptions=true (default).
     * This means if a nested document fails to parse, the error is recorded in its metadata
     * and parsing continues with remaining documents. The container document and other 
     * embedded documents will still be processed successfully.
     *
     * @param stream input stream to parse
     * @param maxLength maximum content length for each document
     * @param pdfConfig PDF parser configuration
     * @param officeConfig Office parser configuration
     * @param tesseractConfig OCR configuration
     * @return RecursiveResult containing list of Metadata for all documents
     */
    private static RecursiveResult parseRecursive(
            TikaInputStream stream,
            int maxLength,
            PDFParserConfig pdfConfig,
            OfficeParserConfig officeConfig,
            TesseractOCRConfig tesseractConfig,
            boolean asXml
    ) throws IOException, TikaException, SAXException {
        try (stream) {
            final TikaConfig config = TikaConfig.getDefaultConfig();
            final ParseContext parseContext = new ParseContext();
            final AutoDetectParser autoParser = new AutoDetectParser(config);

            // Use default constructor: catchEmbeddedExceptions = true
            // This ensures embedded document errors don't fail the entire parse
            final RecursiveParserWrapper wrapper = new RecursiveParserWrapper(autoParser);

            // Configure parse context
            parseContext.set(Parser.class, autoParser);
            parseContext.set(PDFParserConfig.class, pdfConfig);
            parseContext.set(OfficeParserConfig.class, officeConfig);
            parseContext.set(TesseractOCRConfig.class, tesseractConfig);

            // Create handler for recursive parsing
            BasicContentHandlerFactory.HANDLER_TYPE handlerType = asXml
                    ? BasicContentHandlerFactory.HANDLER_TYPE.XML
                    : BasicContentHandlerFactory.HANDLER_TYPE.TEXT;
            BasicContentHandlerFactory factory = new BasicContentHandlerFactory(handlerType, maxLength);
            RecursiveParserWrapperHandler handler = new RecursiveParserWrapperHandler(factory);

            // Parse the document
            wrapper.parse(stream, handler, new Metadata(), parseContext);

            // Get the list of all metadata (container + embedded documents)
            List<Metadata> metadataList = handler.getMetadataList();

            return new RecursiveResult(metadataList);

        }
    }

}
