package com.homegenie.maintenanceservice.service;

public interface S3Service {

    ImageUploadResult uploadImage(String base64Image);

    boolean isS3Available();

    void syncLocalImagesToS3();

    void deleteImage(String imageUrl);

    class ImageUploadResult {
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

        public String getUrl() { return url; }
        public String getStorageType() { return storageType; }
        public String getFileName() { return fileName; }
        public boolean isNeedsSync() { return needsSync; }
    }
}
