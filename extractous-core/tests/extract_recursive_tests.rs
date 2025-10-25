use extractous::Extractor;
use std::fs;

#[cfg(test)]
mod test_utils;

#[test]
fn test_extract_file_recursive_simple_text() {
    // 测试简单文本文件（无嵌套文档）
    let extractor = Extractor::new();
    let result = extractor
        .extract_file_recursive("../test_files/documents/category-level.docx");
    
    // 打印详细错误信息
    if let Err(ref e) = result {
        eprintln!("❌ 提取失败: {:?}", e);
        match e {
            extractous::Error::IoError(msg) => eprintln!("   IO错误: {}", msg),
            extractous::Error::ParseError(msg) => eprintln!("   解析错误: {}", msg),
            extractous::Error::JniError(jni_err) => eprintln!("   JNI错误: {:?}", jni_err),
            _ => eprintln!("   其他错误: {:?}", e),
        }
    }
    
    let result = result.unwrap();

    // 应该至少有一个文档（容器本身）
    assert!(result.total_count() >= 1);
    
    let container = result.container().unwrap();
    assert!(container.content.len() > 0);
    assert!(container.metadata.len() > 0);
    
    println!("总文档数: {}", result.total_count());
    println!("容器文档内容长度: {}", container.content.len());
}

#[test]
fn test_extract_file_recursive_structure() {
    // 测试返回结构
    let extractor = Extractor::new();
    let result = extractor
        .extract_file_recursive("../test_files/documents/simple.odt")
        .unwrap();

    // 至少有容器文档
    assert!(result.total_count() >= 1);
    
    // 容器文档应该存在
    let container = result.container();
    assert!(container.is_some());
    
    // 打印信息用于调试
    println!("总文档数: {}", result.total_count());
    println!("嵌套文档数: {}", result.embedded_documents().len());
    
    for (i, doc) in result.documents.iter().enumerate() {
        println!("文档 {}: 内容长度={}, 元数据数量={}", 
                 i, doc.content.len(), doc.metadata.len());
    }
}

#[test]
fn test_extract_bytes_recursive() {
    // 测试字节数组递归解析
    let file_content = fs::read("../test_files/documents/2022_Q3_AAPL.pdf").unwrap();
    
    let extractor = Extractor::new();
    let result = extractor
        .extract_bytes_recursive(&file_content)
        .unwrap();

    assert!(result.total_count() >= 1);
    assert!(result.container().is_some());
    
    println!("PDF 文档总数: {}", result.total_count());
}

#[test]
fn test_recursive_with_config() {
    // 测试配置传递
    let extractor = Extractor::new()
        .set_extract_string_max_length(5000);
    
    let result = extractor
        .extract_file_recursive("../test_files/documents/2022_Q3_AAPL.pdf")
        .unwrap();

    assert!(result.total_count() >= 1);
    
    // 验证内容长度限制生效
    for doc in result.documents.iter() {
        assert!(doc.content.len() <= 5000, 
                "文档内容长度 {} 超过限制 5000", doc.content.len());
    }
}

#[test]
fn test_recursive_error_handling() {
    // 测试错误处理
    let extractor = Extractor::new();
    let result = extractor.extract_file_recursive("nonexistent_file.txt");
    
    assert!(result.is_err());
}

#[test]
fn test_recursive_embedded_documents_accessor() {
    // 测试嵌套文档访问器
    let extractor = Extractor::new();
    let result = extractor
        .extract_file_recursive("../test_files/documents/simple.odt")
        .unwrap();

    // 测试 container() 方法
    let container = result.container();
    assert!(container.is_some());
    
    // 测试 embedded_documents() 方法
    let embedded = result.embedded_documents();
    assert_eq!(embedded.len(), result.total_count().saturating_sub(1));
    
    // 测试 total_count() 方法
    assert_eq!(result.total_count(), result.documents.len());
}
