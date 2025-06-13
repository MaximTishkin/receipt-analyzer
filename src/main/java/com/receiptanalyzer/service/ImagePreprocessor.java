package com.receiptanalyzer.service;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.awt.image.BufferedImage;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

@Service
public class ImagePreprocessor {

    @Value("${receipt.preprocessing.denoise.strength:25}")
    private int denoiseStrength;

    @Value("${receipt.preprocessing.contrast.limit:1.5}")
    private double contrastLimit;

    private final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
    private final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();

    public BufferedImage preprocessImage(BufferedImage originalImage) {
        try (Mat source = bufferedImageToMat(originalImage)) {
            // Конвертируем в оттенки серого
            Mat gray = new Mat();
            cvtColor(source, gray, COLOR_BGR2GRAY);
            Mat current = gray;

            // Применяем билатеральный фильтр для удаления шума с сохранением границ
            Mat denoised = new Mat();
            bilateralFilter(current, denoised, 5, denoiseStrength, denoiseStrength);
            current = denoised;

            // Улучшаем контраст используя CLAHE
            Mat clahe = new Mat();
            CLAHE claheFilter = createCLAHE(contrastLimit, new Size(8, 8));
            claheFilter.apply(current, clahe);
            current = clahe;

            // Применяем адаптивную бинаризацию
            Mat binary = new Mat();
            adaptiveThreshold(
                    current,
                    binary,
                    255,
                    ADAPTIVE_THRESH_GAUSSIAN_C,
                    THRESH_BINARY,
                    11, // Увеличили размер окна для лучшей работы с текстом
                    10   // Увеличили константу для уменьшения шума
            );
            current = binary;

            // Исправляем наклон
            current = deskewImage(current);

            // Дополнительная обработка для чеков
            current = enhanceReceipt(current);

            return matToBufferedImage(current);
        }
    }

    private Mat deskewImage(Mat binary) {
        try {
            // Создаем копию входного изображения
            Mat binaryClone = binary.clone();
            
            // Находим все контуры
            MatVector contours = new MatVector();
            Mat hierarchy = new Mat();
            findContours(binaryClone, contours, hierarchy, RETR_LIST, CHAIN_APPROX_SIMPLE);
            binaryClone.release();

            try {
                // Находим самый большой контур
                double maxArea = 0;
                RotatedRect bestRect = null;
                double imageArea = binary.size().width() * binary.size().height();

                for (long i = 0; i < contours.size(); i++) {
                    Mat contour = contours.get(i);
                    double area = contourArea(contour);
                    
                    // Проверяем, что площадь контура достаточно большая
                    // (не менее 10% от площади изображения)
                    if (area > maxArea && area > imageArea * 0.1) {
                        maxArea = area;
                        bestRect = minAreaRect(contour);
                    }
                }

                if (bestRect != null) {
                    double angle = bestRect.angle();
                    
                    // Нормализуем угол в диапазон [-45, 45]
                    while (angle < -45) {
                        angle += 90;
                    }
                    while (angle > 45) {
                        angle -= 90;
                    }

                    // Применяем коррекцию наклона только если угол в допустимом диапазоне
                    if (Math.abs(angle) > 0.5 && Math.abs(angle) < 45) {
                        Point2f center = bestRect.center();
                        Mat rotMatrix = getRotationMatrix2D(center, angle, 1.0);
                        Mat rotated = new Mat();
                        Size size = binary.size();
                        
                        // Вычисляем новый размер изображения после поворота
                        double cos = Math.abs(Math.cos(Math.toRadians(angle)));
                        double sin = Math.abs(Math.sin(Math.toRadians(angle)));
                        int newWidth = (int) (size.width() * cos + size.height() * sin);
                        int newHeight = (int) (size.width() * sin + size.height() * cos);
                        
                        // Корректируем матрицу преобразования
                        rotMatrix.ptr(0, 2).putDouble(rotMatrix.ptr(0, 2).getDouble() + (newWidth - size.width()) / 2);
                        rotMatrix.ptr(1, 2).putDouble(rotMatrix.ptr(1, 2).getDouble() + (newHeight - size.height()) / 2);
                        
                        // Применяем поворот
                        warpAffine(binary, rotated, rotMatrix, new Size(newWidth, newHeight));
                        rotMatrix.release();
                        return rotated;
                    }
                }

                return binary;
            } finally {
                // Освобождаем ресурсы
                hierarchy.release();
                for (long i = 0; i < contours.size(); i++) {
                    Mat contour = contours.get(i);
                    if (contour != null && !contour.isNull()) {
                        contour.release();
                    }
                }
                contours.deallocate(); // Используем deallocate вместо release для MatVector
            }
        } catch (Exception e) {
            return binary;
        }
    }

    private Mat enhanceReceipt(Mat image) {
        try {
            // Морфологические операции для улучшения текста
            Mat kernel = getStructuringElement(MORPH_RECT, new Size(1, 1)); // Уменьшили размер ядра
            Mat enhanced = new Mat();

            // Удаляем мелкий шум
            morphologyEx(image, enhanced, MORPH_OPEN, kernel);

            return enhanced; // Убрали дополнительное усиление текста
        } catch (Exception e) {
            return image;
        }
    }

    private Mat bufferedImageToMat(BufferedImage image) {
        return matConverter.convert(java2DConverter.convert(image));
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        return java2DConverter.convert(matConverter.convert(mat));
    }
}