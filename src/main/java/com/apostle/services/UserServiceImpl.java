package com.apostle.services;

import com.apostle.data.model.User;
import com.apostle.data.repositories.UserRepository;
import com.apostle.exceptions.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService{
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;

    public UserServiceImpl(CloudinaryService cloudinaryService, UserRepository userRepository) {
        this.cloudinaryService = cloudinaryService;
        this.userRepository = userRepository;
    }

    @Override
    public String uploadProfilePicture(String userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String imageUrl = cloudinaryService.uploadFile(file);
        user.setProfileImagePath(imageUrl);
        userRepository.save(user);

        return imageUrl;
    }
}
