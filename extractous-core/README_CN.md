# Extractous

Extractous 是一个 Rust crate，提供了统一的方法来检测和提取各种文档类型（如 PDF、Word、HTML 和[许多其他格式](#supported-file-formats)）的元数据和文本内容。

## 特性

* 高级 Rust API，用于提取[多种文件格式](#supported-file-formats)的文本和元数据内容
* 追求高效和快速
* 内部调用 [Apache Tika](https://tika.apache.org/) 处理 Rust 核心不原生支持的文件格式
* 全面的文档和示例，帮助您快速上手

## 使用方法

* 创建和配置 `Extractor` 实例
```rust
use extractous::Extractor;
use extractous::PdfParserConfig;

fn main() {
    // 创建新的提取器。注意它使用消费型构建器模式
    let mut extractor = Extractor::new()
        .set_extract_string_max_length(1000);

    // 也可以执行条件配置
    let custom_pdf_config = true;
    if custom_pdf_config {
        extractor = extractor.set_pdf_config(
            PdfParserConfig::new().set_extract_annotation_text(false)
        );
    }
}
```

* 提取文件内容到 `String`
```rust
use extractous::Extractor;

fn main() {
  // 获取命令行参数
  let args: Vec<String> = std::env::args().collect();
  let file_path = &args[1];

  // 提取指定文件内容到字符串
  let mut extractor = Extractor::new();
  // 如果需要 XML 格式
  // extractor = extractor.set_xml_output(false);
  // 从文件提取文本
  let (content, metadata) = extractor.extract_file_to_string(file_path).unwrap();
  println!("{}", content);
  println!("{:?}", metadata);
}
```

* 提取文件（URL/字节）内容到 `StreamReader` 并执行缓冲读取
```rust
use std::io::{BufReader, Read};
// use std::fs::File; 用于字节提取
use extractous::Extractor;

fn main() {
  // 获取命令行参数
  let args: Vec<String> = std::env::args().collect();
  let file_path = &args[1];

  // 提取指定文件内容到字符串
  let extractor = Extractor::new();
  let (stream, metadata) = extractor.extract_file(file_path).unwrap();
  // 从 URL 提取
  // let (stream, metadata) = extractor.extract_url("https://www.google.com/").unwrap();
  // 从字节提取
  // let mut file = File::open(file_path)?;
  // let mut buffer = Vec::new();
  // file.read_to_end(&mut buffer)?;
  // let (stream, metadata) = extractor.extract_bytes(&file_bytes);

  // 因为 stream 实现了 std::io::Read trait，我们可以执行缓冲读取
  // 例如我们可以用它创建一个 BufReader
  let mut reader = BufReader::new(stream);
  let mut buffer = Vec::new();
  reader.read_to_end(&mut buffer).unwrap();

  println!("{}", String::from_utf8(buffer).unwrap());
  println!("{:?}", metadata);
}
```

* 使用 OCR 提取 PDF 内容。您需要安装 Tesseract 及语言包。例如在 Debian 上 `sudo apt install tesseract-ocr tesseract-ocr-deu`
* 如果您遇到 `Parse error occurred : Unable to extract PDF content` 错误，很可能是未安装 OCR 语言包
```rust
use extractous::Extractor;

fn main() {
  let file_path = "../test_files/documents/deu-ocr.pdf";

  let extractor = Extractor::new()
          .set_ocr_config(TesseractOcrConfig::new().set_language("deu"))
          .set_pdf_config(PdfParserConfig::new().set_ocr_strategy(PdfOcrStrategy::OCR_ONLY));
  // 使用提取器提取文件
  let (content, metadata) = extractor.extract_file_to_string(file_path).unwrap();
  println!("{}", content);
  println!("{:?}", metadata);
}
```

* 递归提取所有嵌入文档（例如 Word 文档中的图片、PDF 中的附件）
```rust
use extractous::Extractor;

fn main() {
  let file_path = "../test_files/documents/embedded-docs.docx";

  let extractor = Extractor::new();
  
  // 递归提取所有文档
  let result = extractor.extract_file_recursive(file_path).unwrap();
  
  // 访问容器文档
  if let Some(container) = result.container() {
      println!("容器内容: {}", container.content);
      println!("容器元数据: {:?}", container.metadata);
  }
  
  // 访问所有嵌入文档
  for (i, doc) in result.embedded_documents().iter().enumerate() {
      println!("\n嵌入文档 {}: ", i + 1);
      println!("内容: {}", &doc.content[..100.min(doc.content.len())]);
      println!("元数据: {:?}", doc.metadata);
  }
  
  // 文档总数
  println!("\n文档总数: {}", result.total_count());
}
```

* 使用自定义选项进行递归提取
```rust
use extractous::Extractor;

fn main() {
  let file_path = "../test_files/documents/large-document.pdf";

  let extractor = Extractor::new();
  
  // 使用自定义最大长度和 XML 输出进行提取
  let result = extractor.extract_file_recursive_opt(
      file_path,
      Some(10000),  // max_length
      Some(true)    // as_xml
  ).unwrap();
  
  // 处理所有文档
  for doc in &result.documents {
      println!("内容长度: {}", doc.content.len());
      println!("元数据: {:?}", doc.metadata);
  }
}
```

* 在标准方法中控制嵌入文档提取
```rust
use extractous::Extractor;

fn main() {
  let file_path = "../test_files/documents/document-with-images.docx";

  // 全局启用嵌入文档提取
  let extractor = Extractor::new().set_extract_embedded(true);
  
  // 标准提取现在也会提取嵌入内容
  let (stream, metadata) = extractor.extract_file(file_path).unwrap();
  // 这将从图片、嵌入文件等中提取文本
}
```


## 构建

### 系统要求
* Extractous 使用 [Apache Tika](https://tika.apache.org/) 处理 Rust 不原生支持的文件格式。
  但是，为了实现 Extractous 的目标（速度和效率），我们不会将 Tika 设置为服务器或
  运行任何 Java 代码。相反，我们使用 [GraalVm](https://www.graalvm.org/) 将 [Apache Tika](https://tika.apache.org/) 编译为原生共享库，并在 Rust 核心中通过 FFI 调用它们。因此需要 GraalVM 来构建 Tika 原生库。
* 提供的构建脚本已经负责安装所需的 GraalVM JDK。但是，如果您想使用特定的本地版本，可以通过设置 GRAALVM_HOME 环境变量来实现
* 我们推荐使用 [sdkman](https://sdkman.io/install) 安装 GraalVM JDK
* `sdk install java 23.0.1-graalce`
* 要在 IDEA 中使用它，例如在 Ubuntu 上，将 `GRAALVM_HOME=$HOME/.sdkman/candidates/java/23.0.1-graalce` 添加到 `/etc/environment`
* 通过运行 `java -version` 确认 GraalVM 已正确安装。您应该看到类似这样的输出：
```text
openjdk 23.0.1 2024-10-15
OpenJDK Runtime Environment GraalVM CE 23.0.1+11.1 (build 23.0.1+11-jvmci-b01)
OpenJDK 64-Bit Server VM GraalVM CE 23.0.1+11.1 (build 23.0.1+11-jvmci-b01, mixed mode, sharing)
```
* 在 macOS 上，官方 GraalVM JDK 无法处理使用 java awt 的代码。在 macOS 上，我们推荐使用 Bellsoft Liberica NIK
* `sdk install java 24.1.1.r23-nik`
* Extractous 通过 [tesseract](https://github.com/tesseract-ocr/tesseract) 支持 OCR，请确保您的系统上安装了 tesseract，因为如果找不到 tesseract，一些 OCR 测试将会失败。
* `sudo apt install tesseract-ocr`
* 安装您需要的任何语言扩展。例如安装德语和阿拉伯语：
* `sudo apt install tesseract-ocr-deu tesseract-ocr-ara`
* 在 Mac 上
* `brew install tesseract tesseract-lang`

### 构建 Extractous
* 要构建 Extractous，只需运行：
* `cargo build`

### 运行测试
* 要运行测试，只需运行：
* `cargo test`

## API 文档

完整的 API 文档可以通过 cargo 生成：

```bash
cargo doc --open
```

## 主要类型

### Extractor

主要的提取器类，提供所有提取方法。

**配置方法：**
- `set_extract_string_max_length(i32)` - 设置提取字符串的最大长度
- `set_xml_output(bool)` - 启用/禁用 XML 输出
- `set_extract_embedded(bool)` - 启用/禁用嵌入文档提取
- `set_encoding(CharSet)` - 设置输出编码
- `set_pdf_config(PdfParserConfig)` - 配置 PDF 解析器
- `set_office_config(OfficeParserConfig)` - 配置 Office 文档解析器
- `set_ocr_config(TesseractOcrConfig)` - 配置 OCR

**提取方法：**
- `extract_file(file_path)` - 从文件提取到流
- `extract_file_to_string(file_path)` - 从文件提取到字符串
- `extract_bytes(buffer)` - 从字节数组提取
- `extract_url(url)` - 从 URL 提取
- `extract_file_recursive(file_path)` - 递归提取文件及所有嵌入文档
- `extract_file_recursive_opt(file_path, max_length, as_xml)` - 带选项的递归提取

### RecursiveExtraction

递归提取的结果类型。

**方法：**
- `container()` - 获取容器文档（第一个文档）
- `embedded_documents()` - 获取所有嵌入文档的切片
- `total_count()` - 获取文档总数

**字段：**
- `documents` - 所有文档的向量（容器 + 嵌入）

### Document

表示单个提取的文档。

**字段：**
- `content: String` - 文档的文本内容
- `metadata: HashMap<String, String>` - 文档元数据

## 许可证

本项目采用 Apache 2.0 许可证。详见 LICENSE 文件。
