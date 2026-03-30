package J2EE.SportBooingSystem.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file); 
}