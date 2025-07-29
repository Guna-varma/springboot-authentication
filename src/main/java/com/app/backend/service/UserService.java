package com.app.backend.service;

import com.app.backend.dto.UserRequestDTO;
import com.app.backend.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User updateUser(Long id, UserRequestDTO dto);
    void deleteUser(Long id); // hard delete (if ever needed)
    void disableUser(Long id); // soft delete using enabled=false
    User getUserById(Long id);
    Optional<User> getUserByEmail(String email);
    List<User> getUsersByName(String name);
    List<User> getUsersByRole(String roleName);
    List<User> getAllUsers(boolean includeDisabled);
    void updateUserRole(Long userId, String roleName);
}





//package com.app.backend.service;
//
//import com.app.backend.dto.UserRequestDTO;
//import com.app.backend.entity.User;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Pattern;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface UserService {
//    User updateUser(Long id, UserRequestDTO dto);
//    void deleteUser(Long id);
//    User getUserById(Long id);
//    Optional<User> getUserByEmail(String email);
//    List<User> getUsersByName(String name);
//    List<User> getUsersByRole(String roleName);
//
//    List<User> getAllUsers();
//    void updateUserRole(Long userId, @NotBlank(message = "Role name is required") @Pattern(regexp = "^(ADMIN|TUTOR|VOLUNTEER|REGISTERED_USER)$", message = "Invalid role") String roleName);
//}