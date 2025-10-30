# Extractous Python Bindings

This project provides Python bindings for the Extractous library, allowing you to use extractous functionality in
your Python applications.

## Installation

To install the extractous Python bindings, you can use pip:

```bash
pip install extractous
```

## Requirements

- **Python**: 3.8 - 3.13
- **Platforms**: Linux (x86_64, aarch64), Windows (x64), macOS (coming soon)

## Usage

### Basic Text Extraction

Extract a file to string:

```python
from extractous import Extractor

# Create a new extractor
extractor = Extractor()
extractor = extractor.set_extract_string_max_length(1000)
# if you need an xml
# extractor = extractor.set_xml_output(True)

# Extract text from a file
result, metadata = extractor.extract_file_to_string("README.md")
print(result)
print(metadata)
```

### Stream-based Extraction

Extract a file (URL / bytearray) to a buffered stream:

```python
from extractous import Extractor

extractor = Extractor()
# for file
reader, metadata = extractor.extract_file("tests/quarkus.pdf")
# for url
# reader, metadata = extractor.extract_url("https://www.google.com")
# for bytearray
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

### OCR Support

Extract a file with OCR (Requires Tesseract):

```python
from extractous import Extractor, TesseractOcrConfig

# Configure OCR with language
extractor = Extractor().set_ocr_config(TesseractOcrConfig().set_language("deu"))
result, metadata = extractor.extract_file_to_string("document-with-images.pdf")

print(result)
print(metadata)
```

### Recursive Extraction with Embedded Documents

Extract all embedded documents recursively (e.g., images in Word documents, attachments in PDFs):

```python
from extractous import Extractor

# Create extractor
extractor = Extractor()

# Recursively extract all embedded documents
result = extractor.extract_file_recursive("document-with-attachments.docx")

# Access the container document
container = result.container()
print("Container content:", container.content)
print("Container metadata:", container.metadata)

# Access all embedded documents
for i, doc in enumerate(result.embedded_documents()):
    print(f"\nEmbedded document {i + 1}:")
    print("Content:", doc.content[:100])  # First 100 chars
    print("Metadata:", doc.metadata)

# Total document count (container + embedded)
print(f"\nTotal documents: {result.total_count}")
```

### Recursive Extraction with Options

Use options to control extraction behavior:

```python
from extractous import Extractor

extractor = Extractor()

# Extract with custom max length and XML output
result = extractor.extract_file_recursive_opt(
    "large-document.pdf",
    max_length=10000,  # Limit content length
    as_xml=True        # Output as XML
)

# Process documents
for doc in result.documents:
    print(f"Content length: {len(doc.content)}")
    print(f"Metadata: {doc.metadata}")
```

### Control Embedded Document Extraction

Control whether to extract embedded documents in standard extraction methods:

```python
from extractous import Extractor

# Enable embedded document extraction globally
extractor = Extractor().set_extract_embedded(True)

# Now standard extraction methods will also extract embedded content
reader, metadata = extractor.extract_file("document.docx")
# This will extract text from images, embedded files, etc.
```

### JVM Memory Management

Monitor and manage JVM memory usage:

#### Get Memory Usage Statistics

```python
from extractous import get_jvm_memory_usage

# Get current JVM memory usage
memory_info = get_jvm_memory_usage()
print(f"Used: {memory_info['usedMemoryMB']:.2f} MB")
print(f"Free: {memory_info['freeMemoryMB']:.2f} MB")
print(f"Total: {memory_info['totalMemoryMB']:.2f} MB")
print(f"Max: {memory_info['maxMemoryMB']:.2f} MB")
print(f"Usage: {memory_info['usagePercent']:.2f}%")
```

#### Trigger Garbage Collection

```python
from extractous import trigger_jvm_gc

# Manually trigger JVM garbage collection
result = trigger_jvm_gc()
print(f"Success: {result['success']}")
print(f"Freed: {result['freedMemoryMB']} MB")
print(f"Before: {result['beforeMB']} MB")
print(f"After: {result['afterMB']} MB")
```

#### Memory Monitoring Workflow

```python
from extractous import Extractor, get_jvm_memory_usage, trigger_jvm_gc

extractor = Extractor()

# Monitor memory during batch processing
for file_path in file_list:
    # Extract file
    result, metadata = extractor.extract_file_to_string(file_path)
    
    # Check memory usage
    memory_info = get_jvm_memory_usage()
    if memory_info['usagePercent'] > 70.0:
        print(f"⚠️  High memory usage: {memory_info['usagePercent']:.2f}%")
        # Trigger garbage collection
        gc_result = trigger_jvm_gc()
        print(f"✅ GC freed {gc_result['freedMemoryMB']} MB")
```
