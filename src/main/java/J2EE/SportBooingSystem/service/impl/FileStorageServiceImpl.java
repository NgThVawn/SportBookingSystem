package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation = Paths.get("uploads");

    public FileStorageServiceImpl() {
        try {
            Files.createDirectories(rootLocation); 
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage folder", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file");
            }
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path destinationFile = rootLocation.resolve(filename).normalize();
            file.transferTo(destinationFile);
            return "/uploads/" + filename; 
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}