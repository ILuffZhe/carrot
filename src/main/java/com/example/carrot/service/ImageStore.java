package com.example.carrot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 图片落盘组件：保存到 {uploadDir}/{subDir}/{yyyyMM}/{uuid}.{ext}，返回相对访问路径。
 * 空文件返回 null（未上传则不更新图片字段）。
 */
@Service
public class ImageStore {

    private static final Set<String> IMAGE_EXTS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp");

    private final String uploadDir;

    public ImageStore(@Value("${carrot.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * 保存单张图片。
     *
     * @param file   上传文件
     * @param subDir 子目录，如 "rewards" / "tasks"
     * @return 相对 URL，如 /uploads/rewards/202608/xxx.jpg；空文件返回 null
     */
    public String save(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String ext = resolveExt(file);
        String yyyymm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String filename = UUID.randomUUID().toString().replace("-", "");
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize()
                    .resolve(subDir).resolve(yyyymm);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename + "." + ext));
        } catch (IOException e) {
            throw new IllegalArgumentException("图片保存失败，请重试", e);
        }
        return "/uploads/" + subDir + "/" + yyyymm + "/" + filename + "." + ext;
    }

    private String resolveExt(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0) {
                String ext = original.substring(dot + 1).toLowerCase();
                if (IMAGE_EXTS.contains(ext)) {
                    return ext;
                }
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("jpeg")) return "jpg";
            if (contentType.contains("png")) return "png";
            if (contentType.contains("gif")) return "gif";
            if (contentType.contains("webp")) return "webp";
            if (contentType.contains("heic")) return "heic";
        }
        throw new IllegalArgumentException("不支持的图片格式");
    }
}
