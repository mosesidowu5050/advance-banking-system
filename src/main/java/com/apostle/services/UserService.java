package com.apostle.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service
public interface UserService {
    String uploadProfilePicture(String userId, MultipartFile file);

}
