package com.phonestore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    /** Lưu nhiều tệp ảnh sản phẩm từ request upload và trả về danh sách đường dẫn công khai. */
    public List<String> storeProductImages(MultipartFile[] files, MultipartFile singleFile) {
        List<String> urls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    urls.add(storeProductImage(f));
                }
            }
        }
        if (singleFile != null && !singleFile.isEmpty()) {
            urls.add(storeProductImage(singleFile));
        }
        if (urls.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một tệp hình ảnh để tải lên");
        }
        return urls;
    }

    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp hình ảnh tải lên không được rỗng");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Kích thước ảnh tối đa là 5MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng ảnh không hợp lệ. Chỉ chấp nhận: JPG, JPEG, PNG, WEBP, GIF, SVG");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String storedFileName = "sp_" + timestamp + "_" + randomSuffix + "." + extension;

        try {
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedFileName).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new IllegalArgumentException("Tên tệp ảnh không hợp lệ");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/products/" + storedFileName;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu hình ảnh lên thư mục cục bộ: " + e.getMessage(), e);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        String clean = filename.trim();
        int dotIdx = clean.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == clean.length() - 1) {
            return "";
        }
        return clean.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
    }
}
