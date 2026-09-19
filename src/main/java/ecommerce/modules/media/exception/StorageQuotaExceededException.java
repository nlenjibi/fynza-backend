package ecommerce.modules.media.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class StorageQuotaExceededException extends FynzaException {

    public StorageQuotaExceededException(long usedBytes, long quotaBytes) {
        super(String.format("Storage quota exceeded: used %d of %d bytes", usedBytes, quotaBytes),
              HttpStatus.FORBIDDEN,
              "STORAGE_QUOTA_EXCEEDED");
    }
}
