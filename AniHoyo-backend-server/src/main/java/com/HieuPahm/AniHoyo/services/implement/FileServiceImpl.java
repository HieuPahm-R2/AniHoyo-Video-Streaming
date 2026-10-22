package com.HieuPahm.AniHoyo.services.implement;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import com.HieuPahm.AniHoyo.services.IFileService;

/**
 * Stores the uploaded images (posters, thumbnails, avatars, sliders).
 *
 * <p>When SeaweedFS is enabled the bytes go to the object store, keyed as
 * {@code <folder>/<file>} so that {@code /storage/<folder>/<file>} keeps
 * resolving. Otherwise the historical behaviour of writing into
 * {@code hieupham.upload-file.base-uri} on the container's own disk is kept,
 * which is what {@code SEAWEEDFS_ENABLED=false} is for.
 *
 * <p>Either way the method returns just the generated file name: the folder is
 * already part of the public URL, so the database columns and the frontend do
 * not have to change.
 */
@Service
public class FileServiceImpl implements IFileService {
    @Value("${hieupham.upload-file.base-uri}")
    private String baseURI;

    private final SeaweedFSMediaStorageService seaweedFS;

    public FileServiceImpl(SeaweedFSMediaStorageService seaweedFS) {
        this.seaweedFS = seaweedFS;
    }

    @Override
    public void createFolder(String folderName) throws URISyntaxException {
        if (seaweedFS.isEnabled()) {
            // Object stores are flat: the bucket is created once at startup and
            // "<folder>/" is just a key prefix. Nothing to do per upload.
            return;
        }
        URI uri = new URI(folderName);
        Path path = Paths.get(uri);
        File tmpDir = new File(path.toString());
        if (!tmpDir.isDirectory()) {
            try {
                Files.createDirectories(tmpDir.toPath());
                System.out.println(">>> CREATE NEW DIRECTORY SUCCESSFUL, PATH = " + tmpDir.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> SKIP MAKING DIRECTORY, ALREADY EXISTS");
        }
    }

    @Override
    public String storeFile(MultipartFile file, String folderName) throws IOException, URISyntaxException {
        if (seaweedFS.isEnabled()) {
            String finalName = generateName(file);
            seaweedFS.put(file, safeFolder(folderName) + "/" + finalName);
            return finalName;
        }
        return storeLocalFile(file, folderName);
    }

    /**
     * Writes a file straight into the local upload volume, bypassing SeaweedFS.
     *
     * <p>Videos have to go through here when {@code R2_ENABLED=false}:
     * {@code EpisodeService#processVideoLocally} runs FFmpeg on
     * {@code <base-uri>/<folder>/<name>}, while {@link #storeFile} routes to
     * SeaweedFS as soon as that store is enabled — so the encoder would look for
     * the file in a place nothing ever wrote to ("…/uploads/videos/<name>.mp4: No
     * such file or directory" and a FAILED episode).
     */
    public String storeLocalFile(MultipartFile file, String folderName) throws IOException, URISyntaxException {
        String finalName = generateName(file);
        URI uri = new URI(baseURI + folderName + "/" + finalName);
        Path path = Paths.get(uri);
        // Ensure parent directories exist
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            return finalName;
        }
    }

    /** {@code <timestamp>-<name with anything exotic replaced by "_">}. */
    private String generateName(MultipartFile file) {
        String sanitized = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.\\-]", "_");
        return System.currentTimeMillis() + "-" + sanitized;
    }

    /**
     * Keeps caller-supplied folder names inside the bucket: strips traversal
     * and leading separators, so {@code folder} can never escape the prefix.
     */
    private String safeFolder(String folderName) {
        if (!StringUtils.hasText(folderName)) {
            return "misc";
        }
        String cleaned = folderName.replace('\\', '/').replaceAll("[^a-zA-Z0-9._/-]", "_");
        cleaned = cleaned.replaceAll("\\.\\.+", "_").replaceAll("^/+", "").replaceAll("/+$", "");
        return cleaned.isBlank() ? "misc" : cleaned;
    }
}
