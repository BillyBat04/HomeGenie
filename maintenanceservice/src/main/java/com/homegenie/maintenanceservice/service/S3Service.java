package com.homegenie.maintenanceservice.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${local.storage.path:./local_images}")
    private String localStoragePath;

    @Value("${local.storage.base-url:http://localhost:8080/local/images}")
    private String localStorageBaseUrl;

    private AmazonS3 s3Client;

    @PostConstruct
    public void init() {
        // Create local storage directory if it doesn't exist
        try {
            Path path = Paths.get(localStoragePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created local storage directory: {}", localStoragePath);
            }
        } catch (IOException e) {
            log.error("Failed to create local storage directory", e);
        }
    }

    private AmazonS3 getS3Client() {
        if (s3Client == null) {
            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(Regions.fromName(region))
                    .build();
        }
        return s3Client;
    }

    /**
     * Upload image with automatic fallback to local storage if S3 fails
     */
    public ImageUploadResult uploadImage(String base64Image) {
        String fileName = "maintenance/" + UUID.randomUUID().toString() + ".jpg";
        byte[] imageBytes = decodeBase64Image(base64Image);

        // Try S3 first
        try {
            String s3Url = uploadToS3(imageBytes, fileName);
            log.info("Image uploaded to S3 successfully: {}", s3Url);
            return new ImageUploadResult(s3Url, "s3", fileName, false);
        } catch (Exception e) {
            log.error("S3 upload failed, falling back to local storage: {}", e.getMessage());

            // Fallback to local storage
            try {
                String localUrl = saveToLocal(imageBytes, fileName);
                log.info("Image saved to local storage: {}", localUrl);
                return new ImageUploadResult(localUrl, "local", fileName, true);
            } catch (Exception localException) {
                log.error("Local storage also failed", localException);
                throw new RuntimeException("Both S3 and local storage failed: " + localException.getMessage());
            }
        }
    }

    /**
     * Upload directly to S3
     */
    private String uploadToS3(byte[] imageBytes, String fileName) throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(imageBytes.length);
        metadata.setContentType("image/jpeg");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

        PutObjectRequest putRequest = new PutObjectRequest(
                bucketName,
                fileName,
                inputStream,
                metadata
        );

        getS3Client().putObject(putRequest);

        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, fileName);
    }

    /**
     * Save to local filesystem
     */
    private String saveToLocal(byte[] imageBytes, String fileName) throws IOException {
        String simpleFileName = fileName.replace("maintenance/", "");
        Path filePath = Paths.get(localStoragePath, simpleFileName);

        Files.write(filePath, imageBytes);

        return localStorageBaseUrl + "/" + simpleFileName;
    }

    /**
     * Decode base64 image
     */
    private byte[] decodeBase64Image(String base64Image) {
        String base64Data = base64Image;
        if (base64Image.contains(",")) {
            base64Data = base64Image.split(",")[1];
        }
        return Base64.getDecoder().decode(base64Data);
    }

    /**
     * Check if S3 is available
     */
    public boolean isS3Available() {
        try {
            getS3Client().doesBucketExistV2(bucketName);
            return true;
        } catch (Exception e) {
            log.warn("S3 is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sync local images to S3
     */
    public void syncLocalImagesToS3() {
        try {
            File localDir = new File(localStoragePath);
            File[] files = localDir.listFiles();

            if (files == null || files.length == 0) {
                log.info("No local images to sync");
                return;
            }

            int synced = 0;
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        byte[] fileBytes = Files.readAllBytes(file.toPath());
                        String fileName = "maintenance/" + file.getName();
                        String s3Url = uploadToS3(fileBytes, fileName);

                        log.info("Synced {} to S3: {}", file.getName(), s3Url);

                        // Optionally delete local file after successful sync
                        // file.delete();

                        synced++;
                    } catch (Exception e) {
                        log.error("Failed to sync file {}: {}", file.getName(), e.getMessage());
                    }
                }
            }

            log.info("Synced {} images to S3", synced);
        } catch (Exception e) {
            log.error("Failed to sync local images to S3", e);
        }
    }

    /**
     * Delete image from both S3 and local storage
     */
    public void deleteImage(String imageUrl) {
        try {
            if (imageUrl.contains("s3.amazonaws.com")) {
                // Delete from S3
                String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                getS3Client().deleteObject(bucketName, "maintenance/" + fileName);
                log.info("Image deleted from S3: {}", fileName);
            } else if (imageUrl.contains(localStorageBaseUrl)) {
                // Delete from local storage
                String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(localStoragePath, fileName);
                Files.deleteIfExists(filePath);
                log.info("Image deleted from local storage: {}", fileName);
            }
        } catch (Exception e) {
            log.error("Failed to delete image: {}", e.getMessage());
        }
    }

    /**
     * Result class for upload operations
     */
    public static class ImageUploadResult {
        private final String url;
        private final String storageType;
        private final String fileName;
        private final boolean needsSync;

        public ImageUploadResult(String url, String storageType, String fileName, boolean needsSync) {
            this.url = url;
            this.storageType = storageType;
            this.fileName = fileName;
            this.needsSync = needsSync;
        }

        public String getUrl() {
            return url;
        }

        public String getStorageType() {
            return storageType;
        }

        public String getFileName() {
            return fileName;
        }

        public boolean isNeedsSync() {
            return needsSync;
        }
    }
}