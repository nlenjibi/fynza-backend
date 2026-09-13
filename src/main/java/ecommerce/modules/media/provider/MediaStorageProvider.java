package ecommerce.modules.media.provider;

import ecommerce.modules.media.enums.ProviderType;

import java.time.Duration;

public interface MediaStorageProvider {

    ProviderType getProviderType();

    StorageCapabilities getCapabilities();

    UploadSession createUploadSession(UploadRequest request);

    UploadResult verifyUpload(UploadSession session);

    void deleteObject(StorageObject object);

    StorageObjectMetadata getMetadata(StorageObject object);

    String createDownloadUrl(StorageObject object, Duration expiration);
}
