package com.receiptanalyzer.util;

import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class ImageUtils {

    public static BufferedImage multipartFileToBufferedImage(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IOException("Имя файла не указано");
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            BufferedImage img = ImageIO.read(file.getInputStream());
            if (img == null) {
                throw new IOException("Не удалось прочитать изображение. Проверьте корректность файла.");
            }
            return img;
        } else if (lower.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                // Рендерим первую страницу PDF в изображение (dpi = 300 для качества)
                BufferedImage img = pdfRenderer.renderImageWithDPI(0, 300);
                if (img == null) {
                    throw new IOException("Не удалось извлечь изображение из PDF.");
                }
                return img;
            }
        } else {
            throw new IOException("Поддерживаются только файлы JPG и PDF");
        }
    }
} 