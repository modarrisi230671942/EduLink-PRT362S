package za.ac.mycput.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.service.IFileStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stores CVs on disk under {@code edulink.upload-dir/cv}.
 * Security measures:
 * <ul>
 *   <li>Only real PDFs are accepted — the file must start with the "%PDF-" signature,
 *       regardless of its name or declared content type.</li>
 *   <li>Files are saved under a random UUID name, so a user can never choose the path on the server.</li>
 *   <li>Stored names are validated before reading/deleting, preventing path traversal ("../").</li>
 * </ul>
 */
@Service
public class FileStorageServiceImpl implements IFileStorageService {

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes();
    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f\\-]{36}\\.pdf$");

    private final Path cvDirectory;

    public FileStorageServiceImpl(@Value("${edulink.upload-dir}") String uploadDir) {
        this.cvDirectory = Path.of(uploadDir, "cv").toAbsolutePath().normalize();
        try {
            Files.createDirectories(cvDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory " + cvDirectory, e);
        }
    }

    @Override
    public String storeCv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Please choose a PDF file to upload.");
        }
        if (!hasPdfSignature(file)) {
            throw ApiException.badRequest("Only PDF files are accepted.");
        }
        String storedName = UUID.randomUUID() + ".pdf";
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, cvDirectory.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store CV", e);
        }
        return storedName;
    }

    @Override
    public StoredFile loadCv(String storedName, String downloadName) {
        Path path = resolveSafely(storedName);
        if (!Files.isReadable(path)) {
            throw ApiException.notFound("CV file");
        }
        return new StoredFile(new PathResource(path), sanitiseDownloadName(downloadName));
    }

    @Override
    public void deleteCv(String storedName) {
        if (storedName == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafely(storedName));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete CV", e);
        }
    }

    private Path resolveSafely(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) {
            throw ApiException.notFound("CV file");
        }
        Path path = cvDirectory.resolve(storedName).normalize();
        if (!path.startsWith(cvDirectory)) {
            throw ApiException.notFound("CV file");
        }
        return path;
    }

    private static boolean hasPdfSignature(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(PDF_SIGNATURE.length);
            if (header.length < PDF_SIGNATURE.length) {
                return false;
            }
            for (int i = 0; i < PDF_SIGNATURE.length; i++) {
                if (header[i] != PDF_SIGNATURE[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Keeps only safe characters for the Content-Disposition header and ensures a .pdf extension. */
    static String sanitiseDownloadName(String name) {
        String base = (name == null || name.isBlank()) ? "cv" : name.replaceAll("(?i)\\.pdf$", "");
        base = base.replaceAll("[^A-Za-z0-9 ._-]", "_").trim();
        if (base.isEmpty()) {
            base = "cv";
        }
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }
        return base + ".pdf";
    }
}
