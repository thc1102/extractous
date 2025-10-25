use crate::errors::ExtractResult;
use crate::tika;
use crate::tika::JReaderInputStream;
use crate::{OfficeParserConfig, PdfParserConfig, TesseractOcrConfig};
use std::collections::HashMap;
use strum_macros::{Display, EnumString};

/// Metadata type alias
pub type Metadata = HashMap<String, Vec<String>>;

/// 单个文档（容器文档或嵌套文档）
#[derive(Debug, Clone)]
pub struct Document {
    /// 文档内容文本
    pub content: String,
    /// 文档元数据
    pub metadata: Metadata,
}

impl Document {
    pub fn new(content: String, metadata: Metadata) -> Self {
        Self { content, metadata }
    }
}

/// 递归提取结果，包含容器文档及其所有嵌套文档
#[derive(Debug, Clone)]
pub struct RecursiveExtraction {
    /// 文档列表：
    /// - documents[0]: 容器文档本身
    /// - documents[1..]: 嵌套文档（如附件、嵌入对象等）
    pub documents: Vec<Document>,
}

impl RecursiveExtraction {
    pub fn new(documents: Vec<Document>) -> Self {
        Self { documents }
    }

    /// 获取容器文档（第一个文档）
    pub fn container(&self) -> Option<&Document> {
        self.documents.first()
    }

    /// 获取所有嵌套文档（跳过容器）
    pub fn embedded_documents(&self) -> &[Document] {
        if self.documents.len() > 1 {
            &self.documents[1..]
        } else {
            &[]
        }
    }

    /// 获取文档总数（容器 + 嵌套）
    pub fn total_count(&self) -> usize {
        self.documents.len()
    }
}

/// CharSet enum of all supported encodings
#[derive(Debug, Clone, Default, Copy, PartialEq, Eq, Hash, Display, EnumString)]
#[allow(non_camel_case_types)]
pub enum CharSet {
    #[default]
    UTF_8,
    US_ASCII,
    UTF_16BE,
}

/// StreamReader implements std::io::Read
///
/// Can be used to perform buffered reading. For example:
/// ```rust
/// use extractous::{CharSet, Extractor};
/// use std::io::BufReader;
/// use std::io::prelude::*;
///
/// let extractor = Extractor::new();
/// let (reader, metadata) = extractor.extract_file("README.md").unwrap();
///
/// let mut buf_reader = BufReader::new(reader);
/// let mut content = String::new();
/// buf_reader.read_to_string(&mut content).unwrap();
/// println!("{}", content);
/// ```
///
pub struct StreamReader {
    pub(crate) inner: JReaderInputStream,
}

impl std::io::Read for StreamReader {
    fn read(&mut self, buf: &mut [u8]) -> std::io::Result<usize> {
        self.inner.read(buf)
    }
}

/// Extractor for extracting text from different file formats
///
/// The Extractor uses the builder pattern to set configurations. This allows configuring and
/// extracting text in one line. For example
/// ```rust
/// use extractous::{CharSet, Extractor};
/// let (text, metadata) = Extractor::new()
///             .set_extract_string_max_length(1000)
///             .extract_file_to_string("README.md")
///             .unwrap();
/// println!("{}", text);
/// ```
///
#[derive(Debug, Clone)]
pub struct Extractor {
    extract_string_max_length: i32,
    encoding: CharSet,
    pdf_config: PdfParserConfig,
    office_config: OfficeParserConfig,
    ocr_config: TesseractOcrConfig,
    xml_output: bool,
    extract_embedded: bool,
}

impl Default for Extractor {
    fn default() -> Self {
        Self {
            extract_string_max_length: -1, // -1 means no limit
            encoding: CharSet::UTF_8,
            pdf_config: PdfParserConfig::default(),
            office_config: OfficeParserConfig::default(),
            ocr_config: TesseractOcrConfig::default(),
            xml_output: false,
            extract_embedded: true,
        }
    }
}

impl Extractor {
    pub fn new() -> Self {
        Self::default()
    }

    /// Set the maximum length of the extracted text. Used only for extract_to_string functions
    /// Default: 500_000
    pub fn set_extract_string_max_length(mut self, max_length: i32) -> Self {
        self.extract_string_max_length = max_length;
        self
    }

    /// Set the encoding to use for when extracting text to a stream.
    /// Not used for extract_to_string functions.
    /// Default: CharSet::UTF_8
    pub fn set_encoding(mut self, encoding: CharSet) -> Self {
        self.encoding = encoding;
        self
    }

    /// Set the configuration for the PDF parser
    pub fn set_pdf_config(mut self, config: PdfParserConfig) -> Self {
        self.pdf_config = config;
        self
    }

    /// Set the configuration for the Office parser
    pub fn set_office_config(mut self, config: OfficeParserConfig) -> Self {
        self.office_config = config;
        self
    }

    /// Set the configuration for the Tesseract OCR
    pub fn set_ocr_config(mut self, config: TesseractOcrConfig) -> Self {
        self.ocr_config = config;
        self
    }

    /// Set the configuration for the parse as xml (global default). Per-call overrides exist via *_opt APIs.
    pub fn set_xml_output(mut self, xml_output: bool) -> Self {
        self.xml_output = xml_output;
        self
    }

    /// Set whether to parse embedded documents (global default). Per-call overrides exist via *_opt APIs.
    /// Default: false
    pub fn set_extract_embedded(mut self, extract_embedded: bool) -> Self {
        self.extract_embedded = extract_embedded;
        self
    }

    /// Extracts text from a file path. Returns a tuple with stream of the extracted text and metadata.
    /// the stream is decoded using the extractor's `encoding`
    pub fn extract_file(&self, file_path: &str) -> ExtractResult<(StreamReader, Metadata)> {
        tika::parse_file(
            file_path,
            &self.encoding,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            self.xml_output,
            self.extract_embedded,
        )
    }

    /// Extracts to stream using optional overrides. If an option is None, uses Extractor defaults.
    pub fn extract_file_opt(
        &self,
        file_path: &str,
        encoding: Option<CharSet>,
        as_xml: Option<bool>,
        extract_embedded: Option<bool>,
    ) -> ExtractResult<(StreamReader, Metadata)> {
        let eff_encoding = encoding.unwrap_or(self.encoding);
        let eff_as_xml = as_xml.unwrap_or(self.xml_output);
        let eff_extract_embedded = extract_embedded.unwrap_or(self.extract_embedded);
        tika::parse_file(
            file_path,
            &eff_encoding,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            eff_as_xml,
            eff_extract_embedded,
        )
    }

    /// Extracts text from a byte buffer. Returns a tuple with stream of the extracted text and metadata.
    /// the stream is decoded using the extractor's `encoding`
    pub fn extract_bytes(&self, buffer: &[u8]) -> ExtractResult<(StreamReader, Metadata)> {
        tika::parse_bytes(
            buffer,
            &self.encoding,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            self.xml_output,
            self.extract_embedded,
        )
    }

    /// Extracts bytes to stream using optional overrides. If an option is None, uses Extractor defaults.
    pub fn extract_bytes_opt(
        &self,
        buffer: &[u8],
        encoding: Option<CharSet>,
        as_xml: Option<bool>,
        extract_embedded: Option<bool>,
    ) -> ExtractResult<(StreamReader, Metadata)> {
        let eff_encoding = encoding.unwrap_or(self.encoding);
        let eff_as_xml = as_xml.unwrap_or(self.xml_output);
        let eff_extract_embedded = extract_embedded.unwrap_or(self.extract_embedded);
        tika::parse_bytes(
            buffer,
            &eff_encoding,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            eff_as_xml,
            eff_extract_embedded,
        )
    }

    /// Extracts text from an url. Returns a tuple with stream of the extracted text and metadata.
    /// the stream is decoded using the extractor's `encoding`
    pub fn extract_url(&self, url: &str) -> ExtractResult<(StreamReader, Metadata)> {
        tika::parse_url(
            url,
            &self.encoding,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            self.xml_output,
            self.extract_embedded,
        )
    }

    /// Extracts url to stream using optional overrides. If an option is None, uses Extractor defaults.
    pub fn extract_url_opt(
        &self,
        url: &str,
        encoding: Option<CharSet>,
        as_xml: Option<bool>,
        extract_embedded: Option<bool>,
    ) -> ExtractResult<(StreamReader, Metadata)> {
        let eff_encoding = encoding.unwrap_or(self.encoding);
        let eff_as_xml = as_xml.unwrap_or(self.xml_output);
        let eff_extract_embedded = extract_embedded.unwrap_or(self.extract_embedded);
        tika::parse_url(
            url,
            &eff_encoding,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            eff_as_xml,
            eff_extract_embedded,
        )
    }

    /// Extracts text from a file path. Returns a tuple with string that is of maximum length
    /// of the extractor's `extract_string_max_length` and metadata.
    pub fn extract_file_to_string(&self, file_path: &str) -> ExtractResult<(String, Metadata)> {
        tika::parse_file_to_string(
            file_path,
            self.extract_string_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            self.xml_output,
            self.extract_embedded,
        )
    }

    /// String extraction with optional overrides (max_length, as_xml, extract_embedded)
    pub fn extract_file_to_string_opt(
        &self,
        file_path: &str,
        max_length: Option<i32>,
        as_xml: Option<bool>,
        extract_embedded: Option<bool>,
    ) -> ExtractResult<(String, Metadata)> {
        let eff_max_length = max_length.unwrap_or(self.extract_string_max_length);
        let eff_as_xml = as_xml.unwrap_or(self.xml_output);
        let eff_extract_embedded = extract_embedded.unwrap_or(self.extract_embedded);
        tika::parse_file_to_string(
            file_path,
            eff_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            eff_as_xml,
            eff_extract_embedded,
        )
    }

    /// Extracts text from a byte buffer. Returns a tuple with string that is of maximum length
    /// of the extractor's `extract_string_max_length` and metadata.
    pub fn extract_bytes_to_string(&self, buffer: &[u8]) -> ExtractResult<(String, Metadata)> {
        tika::parse_bytes_to_string(
            buffer,
            self.extract_string_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            self.xml_output,
            self.extract_embedded,
        )
    }

    pub fn extract_bytes_to_string_opt(
        &self,
        buffer: &[u8],
        max_length: Option<i32>,
        as_xml: Option<bool>,
        extract_embedded: Option<bool>,
    ) -> ExtractResult<(String, Metadata)> {
        let eff_max_length = max_length.unwrap_or(self.extract_string_max_length);
        let eff_as_xml = as_xml.unwrap_or(self.xml_output);
        let eff_extract_embedded = extract_embedded.unwrap_or(self.extract_embedded);
        tika::parse_bytes_to_string(
            buffer,
            eff_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            eff_as_xml,
            eff_extract_embedded,
        )
    }
    /// Extracts text from a URL. Returns a tuple with string that is of maximum length
    /// of the extractor's `extract_string_max_length` and metadata.
    pub fn extract_url_to_string(&self, url: &str) -> ExtractResult<(String, Metadata)> {
        tika::parse_url_to_string(
            url,
            self.extract_string_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            self.xml_output,
            self.extract_embedded,
        )
    }

    pub fn extract_url_to_string_opt(
        &self,
        url: &str,
        max_length: Option<i32>,
        as_xml: Option<bool>,
        extract_embedded: Option<bool>,
    ) -> ExtractResult<(String, Metadata)> {
        let eff_max_length = max_length.unwrap_or(self.extract_string_max_length);
        let eff_as_xml = as_xml.unwrap_or(self.xml_output);
        let eff_extract_embedded = extract_embedded.unwrap_or(self.extract_embedded);
        tika::parse_url_to_string(
            url,
            eff_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            eff_as_xml,
            eff_extract_embedded,
        )
    }

    /// 递归提取文件内容，包括所有嵌套文档
    ///
    /// 返回 RecursiveExtraction，其中：
    /// - documents[0] 是容器文档本身
    /// - documents[1..] 是所有嵌套文档（附件、嵌入对象等）
    ///
    /// # Examples
    /// ```no_run
    /// use extractous::Extractor;
    ///
    /// let extractor = Extractor::new();
    /// let result = extractor.extract_file_recursive("document.zip").unwrap();
    ///
    /// println!("容器文档: {}", result.container().unwrap().content);
    /// println!("嵌套文档数量: {}", result.embedded_documents().len());
    ///
    /// for (i, doc) in result.embedded_documents().iter().enumerate() {
    ///     println!("嵌套文档 {}: {}", i + 1, doc.content);
    /// }
    /// ```
    pub fn extract_file_recursive(&self, file_path: &str) -> ExtractResult<RecursiveExtraction> {
        tika::parse_file_recursive(
            file_path,
            self.extract_string_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            self.xml_output,
        )
    }
    pub fn extract_file_recursive_opt(
        &self,
        file_path: &str,
        max_length: Option<i32>,
        as_xml: Option<bool>,
    ) -> ExtractResult<RecursiveExtraction> {
        let eff_max_length = max_length.unwrap_or(self.extract_string_max_length);
        let eff_as_xml = as_xml.unwrap_or(self.xml_output);
        tika::parse_file_recursive(
            file_path,
            eff_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            eff_as_xml,
        )
    }
    /// 递归提取字节数组内容，包括所有嵌套文档
    pub fn extract_bytes_recursive(&self, buffer: &[u8]) -> ExtractResult<RecursiveExtraction> {
        tika::parse_bytes_recursive(
            buffer,
            self.extract_string_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            self.xml_output,
        )
    }
    pub fn extract_bytes_recursive_opt(
        &self,
        buffer: &[u8],
        max_length: Option<i32>,
        as_xml: Option<bool>,
    ) -> ExtractResult<RecursiveExtraction> {
        let eff_max_length = max_length.unwrap_or(self.extract_string_max_length);
        let eff_as_xml = as_xml.unwrap_or(self.xml_output);
        tika::parse_bytes_recursive(
            buffer,
            eff_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            eff_as_xml,
        )
    }

    /// 递归提取 URL 内容，包括所有嵌套文档
    pub fn extract_url_recursive(&self, url: &str) -> ExtractResult<RecursiveExtraction> {
        tika::parse_url_recursive(
            url,
            self.extract_string_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            self.xml_output,
        )
    }

    pub fn extract_url_recursive_opt(
        &self,
        url: &str,
        max_length: Option<i32>,
        as_xml: Option<bool>,
    ) -> ExtractResult<RecursiveExtraction> {
        let eff_max_length = max_length.unwrap_or(self.extract_string_max_length);
        let eff_as_xml = as_xml.unwrap_or(self.xml_output);
        tika::parse_url_recursive(
            url,
            eff_max_length,
            &self.pdf_config,
            &self.office_config,
            &self.ocr_config,
            eff_as_xml,
        )
    }
}

#[cfg(test)]
mod tests {
    use super::StreamReader;
    use crate::Extractor;
    use std::fs::File;
    use std::io::BufReader;
    use std::io::{self, Read};
    use std::str;

    const TEST_FILE: &str = "README.md";

    const TEST_URL: &str = "https://www.google.com/";

    fn expected_content() -> String {
        let mut file = File::open(TEST_FILE).unwrap();
        let mut content = String::new();
        file.read_to_string(&mut content).unwrap();
        content
    }

    #[test]
    fn extract_file_to_string_test() {
        // Prepare expected_content
        let expected_content = expected_content();

        // Parse the files using extractous
        let extractor = Extractor::new();
        let result = extractor.extract_file_to_string(TEST_FILE);
        let (content, metadata) = result.unwrap();
        assert_eq!(content.trim(), expected_content.trim());
        assert!(
            metadata.len() > 0,
            "Metadata should contain at least one entry"
        );
    }

    fn read_content_from_stream(stream: StreamReader) -> String {
        let mut reader = BufReader::new(stream);
        let mut buffer = Vec::new();
        reader.read_to_end(&mut buffer).unwrap();

        let content = String::from_utf8(buffer).unwrap();
        content
    }

    #[test]
    fn extract_file_test() {
        // Prepare expected_content
        let expected_content = expected_content();

        // Parse the files using extractous
        let extractor = Extractor::new();
        let result = extractor.extract_file(TEST_FILE);
        let (reader, metadata) = result.unwrap();
        let content = read_content_from_stream(reader);

        assert_eq!(content.trim(), expected_content.trim());
        assert!(
            metadata.len() > 0,
            "Metadata should contain at least one entry"
        );
    }

    fn read_file_as_bytes(path: &str) -> io::Result<Vec<u8>> {
        let mut file = File::open(path)?;
        let mut buffer = Vec::new();
        file.read_to_end(&mut buffer)?;
        Ok(buffer)
    }

    #[test]
    fn extract_bytes_test() {
        // Prepare expected_content
        let expected_content = expected_content();

        // Parse the bytes using extractous
        let file_bytes = read_file_as_bytes(TEST_FILE).unwrap();
        let extractor = Extractor::new();
        let result = extractor.extract_bytes(&file_bytes);
        let (reader, metadata) = result.unwrap();
        let content = read_content_from_stream(reader);

        assert_eq!(content.trim(), expected_content.trim());
        assert!(
            metadata.len() > 0,
            "Metadata should contain at least one entry"
        );
    }

    #[test]
    fn extract_url_test() {
        // Parse url by extractous
        let extractor = Extractor::new();
        let result = extractor.extract_url(&TEST_URL);
        let (reader, metadata) = result.unwrap();
        let content = read_content_from_stream(reader);

        assert!(content.contains("Google"));
        assert!(
            metadata.len() > 0,
            "Metadata should contain at least one entry"
        );
    }

    #[test]
    fn extract_file_to_xml_test() {
        // Prefer per-call override for clarity
        let extractor = Extractor::new();
        let result = extractor.extract_file_to_string_opt(TEST_FILE, None, Some(true), None);
        let (content, metadata) = result.unwrap();
        assert!(content.len() > 0);
        assert!(metadata.len() > 0);
    }
}
