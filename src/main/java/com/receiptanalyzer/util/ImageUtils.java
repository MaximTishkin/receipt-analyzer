package com.receiptanalyzer.util;

import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImageUtils {

public static BufferedImage multipartFileToBufferedImage(MultipartFile file) throws IOException {
        return ImageIO.read(file.getInputStream());
    }

    public static void saveImageToFile(BufferedImage image, String prefix) throws IOException {
        // Создаем имя файла с текущей датой и временем
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = prefix + "_" + timestamp + ".png";
        
        // Создаем директорию debug если её нет
        File debugDir = new File("debug");
        if (!debugDir.exists()) {
            debugDir.mkdir();
        }
        
        // Сохраняем файл
        File outputFile = new File(debugDir, fileName);
        ImageIO.write(image, "PNG", outputFile);
    }
} 