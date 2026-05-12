package com.qingtu.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DocumentParserService {

    @Autowired(required = false)
    private RestTemplate restTemplate;

    public List<String> parseDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return parseByExtension(extension, file.getInputStream());
    }

    /**
     * 通过文件 URL 解析（远程文件）
     */
    public List<String> parseDocument(String fileUrl, String fileType) throws IOException {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("文件 URL 不能为空");
        }

        if (fileType == null || fileType.isBlank()) {
            fileType = extractExtension(fileUrl);
        }

        byte[] fileBytes = downloadFile(fileUrl);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IOException("文件下载失败或文件为空");
        }

        return parseByExtension(fileType.toLowerCase(), new ByteArrayInputStream(fileBytes));
    }

    private String extractExtension(String fileUrl) {
        if (fileUrl == null) return "pdf";
        int lastDot = fileUrl.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileUrl.length() - 1) {
            return fileUrl.substring(lastDot + 1).toLowerCase();
        }
        return "pdf";
    }

    private byte[] downloadFile(String fileUrl) {
        try {
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            ResponseEntity<byte[]> response = restTemplate.getForEntity(fileUrl, byte[].class);
            return response.getBody();
        } catch (Exception e) {
            log.error("文件下载失败: url={}, error={}", fileUrl, e.getMessage());
            return null;
        }
    }

    private List<String> parseByExtension(String extension, InputStream inputStream) throws IOException {
        return switch (extension) {
            case "pdf" -> {
                if (inputStream == null) throw new IOException("PDF 文件流为空");
                yield parsePdf(inputStream);
            }
            case "doc" -> {
                if (inputStream == null) throw new IOException("DOC 文件流为空");
                yield parseText(inputStream);
            }
            case "docx" -> {
                if (inputStream == null) throw new IOException("DOCX 文件流为空");
                yield parseDocx(inputStream);
            }
            case "txt" -> {
                if (inputStream == null) throw new IOException("TXT 文件流为空");
                yield parseText(inputStream);
            }
            case "xlsx", "xls" -> {
                // Excel 格式返回占位符，实际走 ExcelUtil
                yield List.of("[Excel 格式，请使用 ExcelUtil 解析]");
            }
            default -> throw new IllegalArgumentException("不支持的文件格式: " + extension);
        };
    }

    private List<String> parsePdf(InputStream inputStream) throws IOException {
        List<String> chunks = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            chunks.addAll(splitText(text, 800, 100));
        }
        return chunks;
    }

    /** .docx 格式（OOXML） */
    private List<String> parseDocx(InputStream inputStream) throws IOException {
        List<String> chunks = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String pText = paragraph.getText();
                if (pText != null && !pText.trim().isEmpty()) {
                    text.append(pText).append("\n");
                }
            }

            chunks.addAll(splitText(text.toString(), 800, 100));
        }
        return chunks;
    }

    private List<String> parseText(InputStream inputStream) throws IOException {
        String text = new String(inputStream.readAllBytes());
        return splitText(text, 800, 100);
    }

    private List<String> splitText(String text, int maxLen, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        // 先按段落分割
        String[] paragraphs = text.split("\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (currentChunk.length() + para.length() > maxLen) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
                // 保留部分重叠内容
                if (currentChunk.length() > overlap) {
                    currentChunk = new StringBuilder(currentChunk.substring(currentChunk.length() - overlap));
                } else {
                    currentChunk = new StringBuilder();
                }
            }
            currentChunk.append(para).append("\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    public String extractTitle(String content) {
        if (content == null || content.isEmpty()) return "未命名文档";

        // 取第一行作为标题
        String firstLine = content.split("\n")[0].trim();
        if (firstLine.length() > 50) {
            firstLine = firstLine.substring(0, 50) + "...";
        }
        return firstLine.isEmpty() ? "未命名文档" : firstLine;
    }
}