package com.phonestore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProductImageStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final Path uploadDirectory;

    public ProductImageStorageService(
            @Value("${phone-store.upload-dir:uploads/products}") String uploadDirectory
    ) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public List<String> store(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một ảnh.");
        }
        List<PendingImage> pendingImages = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Không thể tải lên tệp ảnh rỗng.");
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Mỗi ảnh không được vượt quá 10 MB.");
            }

            byte[] content = file.getBytes();
            String extension = detectImageExtension(content);
            pendingImages.add(new PendingImage(content, extension));
        }

        Files.createDirectories(uploadDirectory);
        List<String> urls = new ArrayList<>();
        List<Path> createdFiles = new ArrayList<>();
        try {
            for (PendingImage pending : pendingImages) {
                String filename = UUID.randomUUID().toString().replace("-", "") + pending.extension();
                Path destination = uploadDirectory.resolve(filename).normalize();
                if (!destination.getParent().equals(uploadDirectory)) {
                    throw new IllegalArgumentException("Tên tệp ảnh không hợp lệ.");
                }
                Files.write(destination, pending.content(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                createdFiles.add(destination);
                urls.add("/uploads/products/" + filename);
            }
        } catch (IOException e) {
            for (Path createdFile : createdFiles) {
                try {
                    Files.deleteIfExists(createdFile);
                } catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            throw e;
        }
        return urls;
    }

    private String detectImageExtension(byte[] content) {
        if (startsWith(content, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return ".png";
        }
        if (startsWith(content, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return ".jpg";
        }
        if (startsWith(content, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(content, "GIF89a".getBytes(StandardCharsets.US_ASCII))) {
            return ".gif";
        }
        if (content.length >= 12
                && new String(content, 0, 4, StandardCharsets.US_ASCII)
                .equals("RIFF")
                && new String(content, 8, 4, StandardCharsets.US_ASCII)
                .equals("WEBP")) {
            return ".webp";
        }
        throw new IllegalArgumentException("Chỉ chấp nhận ảnh PNG, JPEG, GIF hoặc WEBP hợp lệ.");
    }

    private record PendingImage(byte[] content, String extension) {
    }

    private boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
