use extractous::Extractor;

fn main() {
    let extractor = Extractor::new().set_extract_embedded(false);

    println!("Testing extract_file_to_string...");
    let result = extractor.extract_file_to_string("E:\\0811\\performance_test\\文档文件100个\\文档文件80\\Ritzau.pptx");
    
    match result {
        Ok((content, metadata)) => {
            println!("Success! Content length: {}", content.len());
            println!("Metadata: {:?}", metadata);
        }
        Err(e) => {
            println!("Error: {:?}", e);
            eprintln!("Full error: {}", e);
        }
    }
}
