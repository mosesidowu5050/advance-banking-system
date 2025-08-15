package com.apostle.controllers;

import com.apostle.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{userId}/upload-photo")
    public ResponseEntity<String> uploadProfilePicture(@PathVariable String  userId,
                                                       @RequestParam("file") MultipartFile file) {
        String imageUrl = userService.uploadProfilePicture(userId, file);
        return ResponseEntity.ok(imageUrl);
    }
}
