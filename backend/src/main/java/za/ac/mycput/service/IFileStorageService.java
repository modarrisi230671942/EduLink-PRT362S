package za.ac.mycput.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IFileStorageService {

    /** A file ready to be streamed back to the browser. */
    record StoredFile(Resource resource, String downloadName) {}

    /** Validates that the upload is a real PDF, stores it under a random name, and returns that name. */
    String storeCv(MultipartFile file);

    StoredFile loadCv(String storedName, String downloadName);

    void deleteCv(String storedName);
}
