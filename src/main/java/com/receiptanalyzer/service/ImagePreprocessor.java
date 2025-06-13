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
            // Находим все ненулевые точки
            MatVector contours = new MatVector();
            Mat hierarchy = new Mat();
            findContours(binary.clone(), contours, hierarchy, RETR_LIST, CHAIN_APPROX_SIMPLE);

            // Находим самый большой контур
            double maxArea = 0;
            RotatedRect bestRect = null;

            for (long i = 0; i < contours.size(); i++) {
                Mat contour = contours.get(i);
                double area = contourArea(contour);
                if (area > maxArea) {
                    maxArea = area;
                    bestRect = minAreaRect(contour);
                }
            }

            if (bestRect != null) {
                double angle = bestRect.angle();
                if (angle < -45) {
                    angle = 90 + angle;
                }

                // Применяем коррекцию наклона только если угол больше 1 градуса
                if (Math.abs(angle) > 1.0) {
                    Point2f center = bestRect.center();
                    Mat rotMatrix = getRotationMatrix2D(center, angle, 1.0);
                    Mat rotated = new Mat();
                    Size size = binary.size();
                    warpAffine(binary, rotated, rotMatrix, size);
                    return rotated;
                }
            }

            return binary;
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