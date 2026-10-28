package th.mfu.service;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Pictures uploaded by the admin (announcements, parcels): stored in the database and sent back as is. */
public final class Images {

    private Images() {
    }

    /** True when a file was chosen and the browser says it is a picture. */
    public static boolean isImage(MultipartFile file) {
        return file != null && !file.isEmpty() && file.getContentType() != null && file.getContentType().startsWith("image/");
    }

    public static ResponseEntity<byte[]> response(byte[] data, String contentType) {
        if (data == null || contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(data);
    }
}
