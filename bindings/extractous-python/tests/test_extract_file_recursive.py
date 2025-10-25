import pytest
import os

from extractous import Extractor
from utils import read_file_to_bytearray

# 根据当前工作目录确定测试文件路径
if os.path.exists("test_files/documents"):
    TEST_FILES_BASE = "test_files/documents"
else:
    TEST_FILES_BASE = "../../test_files/documents"


class TestExtractFileRecursive:
    """测试 extract_file_recursive 递归提取功能"""

    def test_extract_file_recursive_basic(self):
        """测试基本的递归提取功能"""
        extractor = Extractor()
        result = extractor.extract_file_recursive(f"{TEST_FILES_BASE}/category-level.docx")

        assert result is not None
        assert result.total_count >= 1

        container = result.container()
        assert container is not None
        assert len(container.content) > 0
        assert len(container.metadata) > 0

    def test_extract_file_recursive_with_pdf(self):
        """测试 PDF 文件的递归提取"""
        extractor = Extractor()
        result = extractor.extract_file_recursive(f"{TEST_FILES_BASE}/2022_Q3_AAPL.pdf")

        assert result.total_count >= 1

        container = result.container()
        assert container is not None
        assert "Apple" in container.content or "AAPL" in container.content

    def test_extract_file_recursive_documents_list(self):
        """测试文档列表访问"""
        extractor = Extractor()
        result = extractor.extract_file_recursive(f"{TEST_FILES_BASE}/simple.odt")

        documents = result.documents
        assert len(documents) >= 1
        assert len(documents) == result.total_count

        for doc in documents:
            assert hasattr(doc, 'content')
            assert hasattr(doc, 'metadata')
            assert isinstance(doc.content, str)
            assert isinstance(doc.metadata, dict)

    def test_extract_file_recursive_embedded_documents(self):
        """测试嵌入文档访问"""
        extractor = Extractor()
        result = extractor.extract_file_recursive(f"{TEST_FILES_BASE}/simple.pptx")

        embedded = result.embedded_documents()
        assert isinstance(embedded, list)
        assert len(embedded) == result.total_count - 1

    def test_extract_file_recursive_with_max_length(self):
        """测试带最大长度限制的递归提取"""
        max_length = 5000
        extractor = Extractor()
        result = extractor.extract_file_recursive_opt(
            f"{TEST_FILES_BASE}/2022_Q3_AAPL.pdf",
            max_length=max_length
        )

        assert result.total_count >= 1

        for doc in result.documents:
            assert len(doc.content) <= max_length, \
                f"文档内容长度 {len(doc.content)} 超过限制 {max_length}"

    def test_extract_file_recursive_as_xml(self):
        """测试以 XML 格式递归提取"""
        extractor = Extractor()
        result = extractor.extract_file_recursive_opt(
            f"{TEST_FILES_BASE}/simple.odt",
            as_xml=True
        )

        assert result.total_count >= 1

        container = result.container()
        assert container is not None
        assert "<" in container.content and ">" in container.content

    def test_extract_file_recursive_with_options(self):
        """测试同时使用 max_length 和 as_xml 选项"""
        max_length = 5000
        extractor = Extractor()
        result = extractor.extract_file_recursive_opt(
            f"{TEST_FILES_BASE}/category-level.docx",
            max_length=max_length,
            as_xml=True
        )

        assert result.total_count >= 1

        for doc in result.documents:
            # XML 输出会包含元数据标签,所以长度可能略大于 max_length
            assert len(doc.content) <= max_length + 1000
            assert "<" in doc.content and ">" in doc.content

    def test_extract_file_recursive_metadata(self):
        """测试元数据提取"""
        extractor = Extractor()
        result = extractor.extract_file_recursive(f"{TEST_FILES_BASE}/vodafone.xlsx")

        container = result.container()
        assert container is not None

        metadata = container.metadata
        assert isinstance(metadata, dict)
        assert len(metadata) > 0

    def test_extract_file_recursive_multiple_documents(self):
        """测试多文档处理"""
        test_files = [
            f"{TEST_FILES_BASE}/simple.odt",
            f"{TEST_FILES_BASE}/simple.pptx",
            f"{TEST_FILES_BASE}/vodafone.xlsx",
        ]

        extractor = Extractor()
        for file_path in test_files:
            result = extractor.extract_file_recursive(file_path)
            assert result.total_count >= 1
            assert result.container() is not None

    def test_extract_file_recursive_error_handling(self):
        """测试错误处理"""
        extractor = Extractor()

        with pytest.raises(Exception):
            extractor.extract_file_recursive("nonexistent_file.txt")

    def test_extract_file_recursive_empty_file(self):
        """测试空文件会抛出异常"""
        import tempfile
        import os

        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as f:
            temp_file = f.name
            f.write("")

        try:
            extractor = Extractor()
            with pytest.raises(Exception) as exc_info:
                extractor.extract_file_recursive(temp_file)
            assert "InputStream must have > 0 bytes" in str(exc_info.value)
        finally:
            os.unlink(temp_file)

    def test_extract_file_recursive_container_is_first_document(self):
        """验证 container() 返回的是第一个文档"""
        extractor = Extractor()
        result = extractor.extract_file_recursive(f"{TEST_FILES_BASE}/simple.doc")

        container = result.container()
        documents = result.documents

        assert container is not None
        assert len(documents) > 0
        assert container.content == documents[0].content
        assert container.metadata == documents[0].metadata

    @pytest.mark.parametrize("file_name", [
        "2022_Q3_AAPL.pdf",
        "science-exploration-1p.pptx",
        "simple.odt",
        "vodafone.xlsx",
        "category-level.docx",
        "simple.doc",
        "simple.pptx",
        "winter-sports.epub",
    ])
    def test_extract_file_recursive_various_formats(self, file_name):
        """测试各种文件格式的递归提取"""
        file_path = f"{TEST_FILES_BASE}/{file_name}"
        extractor = Extractor()
        result = extractor.extract_file_recursive(file_path)

        assert result.total_count >= 1
        assert result.container() is not None
        assert len(result.container().content) > 0

    def test_extract_file_recursive_with_extractor_config(self):
        """测试使用配置的提取器进行递归提取"""
        extractor = Extractor()
        extractor = extractor.set_extract_string_max_length(10000)

        result = extractor.extract_file_recursive(f"{TEST_FILES_BASE}/2022_Q3_AAPL.pdf")

        assert result.total_count >= 1
        for doc in result.documents:
            assert len(doc.content) <= 10000
