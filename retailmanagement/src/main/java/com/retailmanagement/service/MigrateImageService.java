package com.retailmanagement.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

@Service
public class MigrateImageService {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Đường dẫn thư mục uploads local của bạn
    private static final String UPLOAD_DIR = "C:\\Users\\Admin\\Desktop\\code\\main\\retailmanagement\\uploads";

    public void migrateAllImages() {
        // Lấy tất cả ảnh từ DB
        List<Map<String, Object>> images = jdbcTemplate.queryForList(
                "SELECT id, url FROM dbo.images"
        );

        int success = 0, failed = 0;

        for (Map<String, Object> image : images) {
            Long id = ((Number) image.get("id")).longValue();
            String url = (String) image.get("url");

            try {
                // url dạng: /uploads/5a6161b9-36be...jpg
                // Lấy tên file từ url
                String fileName = url.replace("/uploads/", "");
                File file = new File(UPLOAD_DIR + "\\" + fileName);

                if (!file.exists()) {
                    System.out.println("FILE NOT FOUND: " + fileName);
                    failed++;
                    continue;
                }

                // Upload lên Cloudinary
                Map uploadResult = cloudinary.uploader().upload(file,
                        ObjectUtils.asMap(
                                "folder", "tech-store",
                                "public_id", fileName.replaceAll("\\.[^.]+$", "") // bỏ extension
                        )
                );

                String newUrl = uploadResult.get("secure_url").toString();

                // Cập nhật URL mới vào DB
                jdbcTemplate.update(
                        "UPDATE dbo.images SET url = ? WHERE id = ?",
                        newUrl, id
                );

                System.out.println("OK [" + id + "]: " + newUrl);
                success++;

            } catch (Exception e) {
                System.out.println("FAILED [" + id + "]: " + e.getMessage());
                failed++;
            }
        }

        System.out.println("===== DONE =====");
        System.out.println("Success: " + success);
        System.out.println("Failed: " + failed);
    }
}