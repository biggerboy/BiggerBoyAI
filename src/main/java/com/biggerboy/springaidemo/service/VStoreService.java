package com.biggerboy.springaidemo.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 这里模拟加载知识库文件到向量存储中，实际应用中可以根据需要进行修改。
 * 这里只是一个简单的示例，实际应用中需要根据具体情况进行修改。理想情况下有一个专门的服务来处理知识库的加载和更新。
 * 可以考虑使用定时任务或者消息队列来触发知识库的加载和更新。
 * 另外，为了避免重复加载知识库文件，可以使用一个标记文件来记录知识库文件的更新时间。
 * 当知识库文件更新时，删除标记文件，然后重新加载知识库文件。
 * 这样可以避免重复加载知识库文件，提高效率。
 *
 * @author BiggerBoy
 * @date 2025/5/7
 */
@Service
public class VStoreService {
    final Logger logger = LoggerFactory.getLogger(VStoreService.class);
    @Autowired
    private VectorStore vectorStore;

    private static final String MARK_FILE_PATH = "knowledge_base_added.mark";
    private static final String DOCUMENT_FILE_PATH = "src/main/resources/redis8.txt";

    /**
     * 在 Bean 初始化完成后执行，检查知识库文件是否更新，若更新则重新添加到向量存储中，并进行相似度搜索测试。
     */
    @PostConstruct
    public void addDocument() {
        File markFile = new File(MARK_FILE_PATH);
        File documentFile = new File(DOCUMENT_FILE_PATH);

        // 检查文档文件是否存在
        if (!documentFile.exists()) {
            logger.info("知识库文件不存在: " + DOCUMENT_FILE_PATH);
            return;
        }

        // 检查标记文件是否存在，或者文档文件是否已更新
        if (!markFile.exists() || documentFile.lastModified() > markFile.lastModified()) {
            if (markFile.exists()) {
                // 删除旧的标记文件
                markFile.delete();
            }

            try {
                Resource documentResource = new ClassPathResource("redis8.txt");
                DocumentReader documentReader = new TextReader(documentResource);
                List<Document> documents2 = documentReader.get();

                TextSplitter textSplitter = new TokenTextSplitter(); // 按 Token 数分块
                List<Document> chunks = textSplitter.split(documents2);

                vectorStore.add(chunks);

                // 创建新的标记文件
                markFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
