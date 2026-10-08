package za.ac.mycput.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import za.ac.mycput.service.IFileStorageService.StoredFile;

/** Builds file download responses. */
final class FileResponses {

    private FileResponses() {}

    static ResponseEntity<Resource> pdf(StoredFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.downloadName()).build().toString())
                // Stops the browser from treating the file as anything other than a PDF
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }
}
