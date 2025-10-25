# 项目声明
二开 [Extractous](https://github.com/yobix-ai/extractous) 项目，仅供测试使用，不保证可用性


<div align="center" style="margin-top: 20px">
    <a href="https://yobix.ai">
    <img height="28px" alt="yobix ai logo" src="https://framerusercontent.com/images/zaqayjWBWNoQmV9MIwSEKf0HBo.png?scale-down-to=512">
    </a>
<h1 style="margin-top: 0; padding-top: 0">Extractous</h1>
</div>

<div align="center">

<a href="https://github.com/yobix-ai/extractous/blob/main/LICENSE">![https://pypi.python.org/pypi/unstructured/](https://img.shields.io/pypi/l/unstructured.svg)</a>
[![](https://img.shields.io/crates/v/extractous)](https://crates.io/crates/extractous)
[![](https://img.shields.io/pypi/v/extractous)](https://pypi.org/project/extractous/)
<img src="https://img.shields.io/github/commit-activity/m/yobix-ai/extractous" alt="Commits per month">
[![Downloads](https://static.pepy.tech/badge/extractous/month)](https://pepy.tech/project/extractous)

</div>

<div align="center">

_Extractous 为从 PDF、Word、HTML 以及[许多其他格式](#supported-file-formats)等各种文档类型中提取内容和元数据提供了快速高效的解决方案。我们的目标是在 Rust 中提供一个快速高效的综合解决方案，并为多种编程语言提供绑定。_

</div>

---

**演示**：展示 [Extractous 🚀](https://github.com/yobix-ai/extractous) 比流行的 [unstructured-io](https://github.com/Unstructured-IO/unstructured) 库（6500万美元融资，8.5k GitHub stars）**快25倍**。完整基准测试详情请查阅我们的[基准测试仓库](https://github.com/yobix-ai/extractous-benchmarks)

![unstructured_vs_extractous](https://github.com/yobix-ai/extractous-benchmarks/raw/main/docs/extractous_vs_unstructured.gif)
<sup>* 演示以5倍录制速度运行</sup>

## 为什么选择 Extractous？

**Extractous** 诞生于对依赖外部服务或 API 从非结构化数据中提取内容的挫败感。我们真的需要调用外部 API 或运行特殊服务器来提取内容吗？提取难道不能在本地高效地执行吗？

在寻找解决方案的过程中，**unstructured-io** 作为流行且广泛使用的库脱颖而出，可以在进程内解析非结构化内容。然而，我们发现了几个重大局限：

- 在架构上，unstructured-io 包装了众多重量级 Python 库，导致性能缓慢和内存消耗高（详见我们的[基准测试](https://github.com/yobix-ai/extractous-benchmarks)）。
- 在利用多个 CPU 核心处理数据处理任务（主要是 CPU 密集型）方面效率低下。这种低效是由于其依赖项的限制以及全局解释器锁（GIL）等约束，GIL 阻止多个线程同时执行 Python 字节码。
- 随着 unstructured-io 的发展，它变得越来越复杂，正在转变为更复杂的框架，并更多地专注于提供外部 API 服务来进行文本和元数据提取。

相比之下，**Extractous** 保持对文本和元数据提取的专注。它通过原生代码执行实现了明显更快的处理速度和更低的内存利用率。

* **使用 Rust 构建：** 核心使用 Rust 开发，充分利用其高性能、内存安全、多线程能力和零成本抽象。
* **通过 Apache Tika 扩展格式支持：** 对于 Rust 核心不原生支持的文件格式，我们使用 [GraalVM](https://www.graalvm.org/) 提前编译技术将著名的 [Apache Tika](https://tika.apache.org/) 编译为原生共享库。然后从我们的 Rust 核心链接和调用这些共享库。没有本地服务器，没有虚拟机，也没有任何垃圾回收，只有纯原生执行。
* **多语言绑定：** 我们计划为多种语言引入绑定。目前我们仅提供 Python 绑定，它本质上是 Rust 核心的包装器，有潜力绕过 Python GIL 限制并高效利用多核。

使用 Extractous，消除了对外部服务或 API 的需求，使数据处理管道更快、更高效。

## 🌳 主要特性
* 高性能非结构化数据提取，针对速度和低内存使用进行了优化
* 清晰简单的 API，用于提取文本和元数据内容
* 自动识别文档类型并相应提取内容
* **递归提取**嵌入文档（Word 文档中的图片、PDF 中的附件等）
* 支持[多种文件格式](#supported-file-formats)（Apache Tika 支持的大多数格式）
* 通过 [tesseract-ocr](https://github.com/tesseract-ocr/tesseract) 从图片和扫描文档中提取文本
* **可配置的嵌入文档提取控制**，包括深度和选项
* 核心引擎使用 Rust 编写，提供 [Python](https://pypi.org/project/extractous/)（3.8-3.13）绑定，即将支持 JavaScript/TypeScript
* 详细的文档和示例帮助您快速高效地入门
* 商业使用免费：Apache 2.0 许可证

## 🚀 快速开始
Extractous 提供了简单易用的 API 来从各种文件格式中提取内容。以下是快速示例：

#### Python
* 提取文件内容到字符串：
```python
from extractous import Extractor

# 创建新的提取器
extractor = Extractor()
extractor = extractor.set_extract_string_max_length(1000)
# 如果需要 XML 格式
# extractor = extractor.set_xml_output(True)

# 从文件提取文本
result, metadata = extractor.extract_file_to_string("README.md")
print(result)
print(metadata)
```
* 将文件（URL / 字节数组）提取到缓冲流：

```python
from extractous import Extractor

extractor = Extractor()
# 如果需要 XML 格式
# extractor = extractor.set_xml_output(True)

# 从文件提取
reader, metadata = extractor.extract_file("tests/quarkus.pdf")
# 从 URL 提取
# reader, metadata = extractor.extract_url("https://www.google.com")
# 从字节数组提取
# with open("tests/quarkus.pdf", "rb") as file:
#     buffer = bytearray(file.read())
# reader, metadata = extractor.extract_bytes(buffer)

result = ""
buffer = reader.read(4096)
while len(buffer) > 0:
    result += buffer.decode("utf-8")
    buffer = reader.read(4096)

print(result)
print(metadata)
```

* 使用 OCR 提取文件：

您需要安装 Tesseract 及语言包。例如在 Debian 上 `sudo apt install tesseract-ocr tesseract-ocr-deu`

```python
from extractous import Extractor, TesseractOcrConfig

extractor = Extractor().set_ocr_config(TesseractOcrConfig().set_language("deu"))
result, metadata = extractor.extract_file_to_string("document-with-images.pdf")

print(result)
print(metadata)
```

* 递归提取所有嵌入文档：

递归提取所有嵌入文档（例如 Word 文档中的图片、PDF 中的附件）：

```python
from extractous import Extractor

# 创建提取器
extractor = Extractor()

# 递归提取所有嵌入文档
result = extractor.extract_file_recursive("document-with-attachments.docx")

# 访问容器文档
container = result.container()
print("容器内容:", container.content)
print("容器元数据:", container.metadata)

# 访问所有嵌入文档
for i, doc in enumerate(result.embedded_documents()):
    print(f"\n嵌入文档 {i + 1}:")
    print("内容:", doc.content[:100])  # 前 100 个字符
    print("元数据:", doc.metadata)

# 文档总数（容器 + 嵌入）
print(f"\n文档总数: {result.total_count}")
```

#### Rust
* 提取文件内容到字符串：
```rust
use extractous::Extractor;

fn main() {
    // 创建新的提取器。注意它使用消费型构建器模式
    let mut extractor = Extractor::new().set_extract_string_max_length(1000);
    // 如果需要 XML 格式
    // extractor = extractor.set_xml_output(true);

    // 从文件提取文本
    let (text, metadata) = extractor.extract_file_to_string("README.md").unwrap();
    println!("{}", text);
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
    // 如果需要 XML 格式
    // extractor = extractor.set_xml_output(true);

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

* 使用 OCR 提取 PDF 内容。

您需要安装 Tesseract 及语言包。例如在 Debian 上 `sudo apt install tesseract-ocr tesseract-ocr-deu`

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

* 递归提取所有嵌入文档

递归提取所有嵌入文档（例如 Word 文档中的图片、PDF 中的附件）：

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


## 🔥 性能
* **Extractous** 速度很快，请不要只听我们的话，您可以自己运行[基准测试](https://github.com/yobix-ai/extractous-benchmarks)。例如从 [sec10 申报 PDF 表格](https://github.com/yobix-ai/extractous-benchmarks/raw/main/dataset/sec10-filings)中提取内容，Extractous 平均比 unstructured-io **快约18倍**：

![extractous_speedup_relative_to_unstructured](https://github.com/yobix-ai/extractous-benchmarks/raw/main/docs/extractous_speedup_relative_to_unstructured.png)

* 不仅速度快，而且内存效率高，Extractous 分配的内存比 unstructured-io **少约11倍**：

![extractous_memory_efficiency_relative_to_unstructured](https://github.com/yobix-ai/extractous-benchmarks/raw/main/docs/extractous_memory_efficiency_relative_to_unstructured.png)

* 您可能会质疑提取内容的质量，猜猜看，我们在这方面做得甚至更好：

![extractous_memory_efficiency_relative_to_unstructured](https://github.com/yobix-ai/extractous-benchmarks/raw/main/docs/extractous_unstructured_quality_scores.png)

## 📄 支持的文件格式

| **类别**        | **支持的格式**                                   | **备注**                                      |
|---------------------|---------------------------------------------------------|------------------------------------------------|
| **Microsoft Office**| DOC, DOCX, PPT, PPTX, XLS, XLSX, RTF                    | 包括旧版和现代 Office 文件格式 |
| **OpenOffice**      | ODT, ODS, ODP                                           | OpenDocument 格式                           |
| **PDF**             | PDF                                                     | 可提取嵌入内容并支持 OCR |
| **电子表格**    | CSV, TSV                                                | 纯文本电子表格格式                 |
| **网页文档**   | HTML, XML                                               | 解析并从网页文档中提取内容 |
| **电子书**         | EPUB                                                    | EPUB 电子书格式               |
| **文本文件**      | TXT, Markdown                                           | 纯文本格式                             |
| **图片**          | PNG, JPEG, TIFF, BMP, GIF, ICO, PSD, SVG                | 使用 OCR 提取嵌入文本                |
| **电子邮件**          | EML, MSG, MBOX, PST                                     | 提取内容、头部和附件     |

[//]: # (| **压缩包**        | ZIP, TAR, GZIP, RAR, 7Z                                 | 从压缩包中提取内容      |)
[//]: # (| **音频**           | MP3, WAV, OGG, FLAC, AU, MIDI, AIFF, APE                | 提取如 ID3 标签等元数据             |)
[//]: # (| **视频**           | MP4, AVI, MOV, WMV, FLV, MKV, WebM                      | 提取元数据和基本信息        |)
[//]: # (| **CAD 文件**       | DXF, DWG                                                | 支持工程图纸的 CAD 格式  |)
[//]: # (| **其他**           | ICS &#40;日历&#41;, VCF &#40;vCard&#41;                             | 支持日历和联系人文件格式     |)
[//]: # (| **地理空间**      | KML, KMZ, GeoJSON                                       | 提取地理空间数据和元数据          |)
[//]: # (| **字体文件**      | TTF, OTF                                                | 从字体文件中提取元数据              |)

## 🤝 贡献

欢迎贡献！如果您有任何改进或新功能建议，请开启 issue 或提交 pull request。

## 🕮 许可证
本项目采用 Apache License 2.0 许可证。详见 LICENSE 文件。
