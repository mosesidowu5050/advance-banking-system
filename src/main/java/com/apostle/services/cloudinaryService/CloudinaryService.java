package com.apostle.services.cloudinaryService;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface CloudinaryService {
    String uploadFile(MultipartFile file);
}
