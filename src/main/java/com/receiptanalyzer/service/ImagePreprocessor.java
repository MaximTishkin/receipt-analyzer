package com.receiptanalyzer.service;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

@Service
public class ImagePreprocessor {

    @Value("${receipt.preprocessing.denoise.strength:25}")
    private int denoiseStrength;

    @Value("${receipt.preprocessing.contrast.limit:1.5}")
    private double contrastLimit;

    private Mat bufferedImageToMat(BufferedImage image) {
        try {
            // Конвертируем изображение в правильный формат
            BufferedImage convertedImg = new BufferedImage(
                image.getWidth(), 
                image.getHeight(), 
                BufferedImage.TYPE_3BYTE_BGR
            );
            convertedImg.getGraphics().drawImage(image, 0, 0, null);

            // Получаем байты изображения
            byte[] pixels = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();

            // Создаем Mat и копируем данные
            Mat mat = new Mat(convertedImg.getHeight(), convertedImg.getWidth(), CV_8UC3);
            mat.data().put(pixels);

            return mat;
        } catch (Exception e) {
            throw new RuntimeException("Error converting BufferedImage to Mat: " + e.getMessage(), e);
        }
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        try {
            // Определяем тип BufferedImage на основе типа Mat
            int type = BufferedImage.TYPE_BYTE_GRAY;
            if (mat.channels() == 3) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            }

            // Создаем BufferedImage
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);

            // Получаем байты из Mat
            byte[] data = new byte[mat.channels() * mat.cols() * mat.rows()];
            mat.data().get(data);

            // Копируем данные в BufferedImage
            byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(data, 0, targetPixels, 0, data.length);

            return image;
        } catch (Exception e) {
            throw new RuntimeException("Error converting Mat to BufferedImage: " + e.getMessage(), e);
        }
    }

    public BufferedImage preprocessImage(BufferedImage originalImage) {
        Mat source = null;
        Mat gray = null;
        Mat denoised = null;
        Mat clahe = null;
        Mat binary = null;
        Mat result = null;

        try {
            // Конвертируем BufferedImage в Mat
            source = bufferedImageToMat(originalImage);
            if (source == null || source.empty()) {
                throw new IllegalStateException("Failed to convert input image to Mat");
            }

            // Конвертируем в оттенки серого
            gray = new Mat();
            cvtColor(source, gray, COLOR_BGR2GRAY);
            if (gray.empty()) {
                throw new IllegalStateException("Failed to convert image to grayscale");
            }

            // Применяем билатеральный фильтр для удаления шума с сохранением границ
            denoised = new Mat();
            bilateralFilter(gray, denoised, 5, denoiseStrength, denoiseStrength);
            if (denoised.empty()) {
                throw new IllegalStateException("Failed to apply bilateral filter");
            }

            // Улучшаем контраст используя CLAHE
            clahe = new Mat();
            CLAHE claheFilter = createCLAHE(contrastLimit, new Size(8, 8));
            claheFilter.apply(denoised, clahe);
            if (clahe.empty()) {
                throw new IllegalStateException("Failed to apply CLAHE");
            }

            // Применяем адаптивную бинаризацию
            binary = new Mat();
            adaptiveThreshold(
                    clahe,
                    binary,
                    255,
                    ADAPTIVE_THRESH_GAUSSIAN_C,
                    THRESH_BINARY,
                    11,
                    10
            );
            if (binary.empty()) {
                throw new IllegalStateException("Failed to apply adaptive threshold");
            }

            // Исправляем наклон
            result = deskewImage(binary);

            // Дополнительная обработка для чеков
            Mat enhanced = enhanceReceipt(result);
            result.release();
            result = enhanced;

            // Конвертируем обратно в BufferedImage
            return matToBufferedImage(result);

        } catch (Exception e) {
            throw new RuntimeException("Error during image preprocessing: " + e.getMessage(), e);
        } finally {
            // Освобождаем все ресурсы
            if (source != null && !source.isNull()) source.release();
            if (gray != null && !gray.isNull()) gray.release();
            if (denoised != null && !denoised.isNull()) denoised.release();
            if (clahe != null && !clahe.isNull()) clahe.release();
            if (binary != null && !binary.isNull()) binary.release();
            if (result != null && !result.isNull()) result.release();
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
                contours.deallocate();
            }
        } catch (Exception e) {
            return binary;
        }
    }

    private Mat enhanceReceipt(Mat image) {
        try {
            // Морфологические операции для улучшения текста
            Mat kernel = getStructuringElement(MORPH_RECT, new Size(1, 1));
            Mat enhanced = new Mat();

            // Удаляем мелкий шум
            morphologyEx(image, enhanced, MORPH_OPEN, kernel);
            kernel.release();

            return enhanced;
        } catch (Exception e) {
            return image;
        }
    }
}