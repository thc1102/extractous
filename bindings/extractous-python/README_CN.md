# Extractous Python 绑定

本项目为 Extractous 库提供 Python 绑定，让您可以在 Python 应用程序中使用 Extractous 功能。

## 安装

使用 pip 安装 Extractous Python 绑定：

```bash
pip install extractous
```

## 系统要求

- **Python 版本**: 3.8 - 3.13
- **支持平台**: Linux (x86_64, aarch64)、Windows (x64)、macOS（即将支持）

## 使用方法

### 基础文本提取

提取文件内容到字符串：

```python
from extractous import Extractor

# 创建提取器
extractor = Extractor()
extractor = extractor.set_extract_string_max_length(1000)
# 如果需要 XML 格式输出
# extractor = extractor.set_xml_output(True)

# 从文件中提取文本
result, metadata = extractor.extract_file_to_string("README.md")
print(result)
print(metadata)
```

### 基于流的提取

将文件（URL / 字节数组）提取到缓冲流：

```python
from extractous import Extractor

extractor = Extractor()
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

### OCR 支持

使用 OCR 提取文件（需要安装 Tesseract）：

```python
from extractous import Extractor, TesseractOcrConfig

# 配置 OCR 语言
extractor = Extractor().set_ocr_config(TesseractOcrConfig().set_language("deu"))
result, metadata = extractor.extract_file_to_string("document-with-images.pdf")

print(result)
print(metadata)
```

### 递归提取嵌入文档

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

### 使用选项进行递归提取

使用选项控制提取行为：

```python
from extractous import Extractor

extractor = Extractor()

# 使用自定义最大长度和 XML 输出进行提取
result = extractor.extract_file_recursive_opt(
    "large-document.pdf",
    max_length=10000,  # 限制内容长度
    as_xml=True        # 输出为 XML 格式
)

# 处理文档
for doc in result.documents:
    print(f"内容长度: {len(doc.content)}")
    print(f"元数据: {doc.metadata}")
```

### 控制嵌入文档提取

在标准提取方法中控制是否提取嵌入文档：

```python
from extractous import Extractor

# 全局启用嵌入文档提取
extractor = Extractor().set_extract_embedded(True)

# 现在标准提取方法也会提取嵌入内容
reader, metadata = extractor.extract_file("document.docx")
# 这将从图片、嵌入文件等中提取文本
```

### JVM 内存管理

监控和管理 JVM 内存使用：

#### 获取内存使用统计

```python
from extractous import get_jvm_memory_usage

# 获取当前 JVM 内存使用情况
memory_info = get_jvm_memory_usage()
print(f"已使用: {memory_info['usedMemoryMB']:.2f} MB")
print(f"空闲: {memory_info['freeMemoryMB']:.2f} MB")
print(f"总计: {memory_info['totalMemoryMB']:.2f} MB")
print(f"最大: {memory_info['maxMemoryMB']:.2f} MB")
print(f"使用率: {memory_info['usagePercent']:.2f}%")
```

#### 触发垃圾回收

```python
from extractous import trigger_jvm_gc

# 手动触发 JVM 垃圾回收
result = trigger_jvm_gc()
print(f"成功: {result['success']}")
print(f"释放: {result['freedMemoryMB']} MB")
print(f"之前: {result['beforeMB']} MB")
print(f"之后: {result['afterMB']} MB")
```

#### 内存监控工作流

```python
from extractous import Extractor, get_jvm_memory_usage, trigger_jvm_gc

extractor = Extractor()

# 在批量处理期间监控内存
for file_path in file_list:
    # 提取文件
    result, metadata = extractor.extract_file_to_string(file_path)
    
    # 检查内存使用情况
    memory_info = get_jvm_memory_usage()
    if memory_info['usagePercent'] > 70.0:
        print(f"⚠️  内存使用率过高: {memory_info['usagePercent']:.2f}%")
        # 触发垃圾回收
        gc_result = trigger_jvm_gc()
        print(f"✅ GC 释放了 {gc_result['freedMemoryMB']} MB")
```

## API 参考

### 主要方法

- `extract_file_to_string(file_path)` - 提取文件到字符串
- `extract_file(file_path)` - 提取文件到流
- `extract_url(url)` - 从 URL 提取内容
- `extract_bytes(buffer)` - 从字节数组提取
- `extract_file_recursive(file_path)` - 递归提取文件及所有嵌入文档
- `extract_file_recursive_opt(file_path, max_length, as_xml)` - 带选项的递归提取
- `extract_bytes_recursive(buffer)` - 递归提取字节数组
- `extract_url_recursive(url)` - 递归提取 URL

### 配置方法

- `set_extract_string_max_length(length)` - 设置提取字符串的最大长度
- `set_xml_output(enabled)` - 启用/禁用 XML 输出
- `set_extract_embedded(enabled)` - 启用/禁用嵌入文档提取
- `set_ocr_config(config)` - 设置 OCR 配置
- `set_pdf_config(config)` - 设置 PDF 解析配置
- `set_office_config(config)` - 设置 Office 文档解析配置

### RecursiveExtraction 类

递归提取返回的结果对象：

- `container()` - 获取容器文档（第一个文档）
- `embedded_documents()` - 获取所有嵌入文档列表
- `documents` - 获取所有文档（容器 + 嵌入）
- `total_count` - 文档总数

### Document 类

表示提取的单个文档：

- `content` - 文档文本内容
- `metadata` - 文档元数据字典

## 常见问题

### 如何处理大文件？

使用流式提取方法以减少内存使用：

```python
reader, metadata = extractor.extract_file("large-file.pdf")
# 分块读取
buffer = reader.read(4096)
```

### 如何提取特定语言的 OCR？

安装对应的 Tesseract 语言包并配置：

```bash
# Debian/Ubuntu
sudo apt install tesseract-ocr-chi-sim  # 简体中文
sudo apt install tesseract-ocr-chi-tra  # 繁体中文
```

```python
extractor = Extractor().set_ocr_config(
    TesseractOcrConfig().set_language("chi_sim")
)
```

### 支持哪些文件格式？

Extractous 支持大多数常见文档格式，包括：
- Microsoft Office: DOC, DOCX, PPT, PPTX, XLS, XLSX
- PDF 文档
- OpenOffice: ODT, ODS, ODP
- 电子邮件: EML, MSG
- 网页: HTML, XML
- 图片（带 OCR）: PNG, JPEG, TIFF 等

完整列表请参见主 [README](../../README.md#supported-file-formats)。

## 许可证

本项目采用 Apache 2.0 许可证。详见 [LICENSE](../../LICENSE) 文件。
