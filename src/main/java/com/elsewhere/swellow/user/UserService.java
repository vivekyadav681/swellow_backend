package com.elsewhere.swellow.user;

import com.elsewhere.swellow.profile.Profile;
import com.elsewhere.swellow.profile.ProfileRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public UserService(UserRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public Profile getProfile() {
        User user = getCurrentUser();
        return profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
    }

    @Transactional
    public Profile updateProfile(Profile updatedProfile) {
        Profile profile = getProfile();
        if (updatedProfile.getDisplayName() != null && !updatedProfile.getDisplayName().isBlank()) {
            profile.setDisplayName(updatedProfile.getDisplayName());
        }
        if (updatedProfile.getBio() != null) {
            profile.setBio(updatedProfile.getBio());
        }
        if (updatedProfile.getAvatarUrl() != null) {
            profile.setAvatarUrl(updatedProfile.getAvatarUrl());
        }
        return profileRepository.save(profile);
    }
}
