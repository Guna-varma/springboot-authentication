package com.app.backend.service;

import com.app.backend.dto.UserRequestDTO;
import com.app.backend.entity.Role;
import com.app.backend.entity.User;
import com.app.backend.repository.RoleRepository;
import com.app.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional // ✅ Ensure transaction boundary
    public User updateUser(Long id, UserRequestDTO dto) {
        // ✅ Use the optimized query that fetches role with JOIN FETCH
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        // Update user fields
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getProvider() != null) user.setProvider(dto.getProvider());

        // ✅ Handle role update with proper fetching
        if (dto.getRoleId() != null) {
            Role role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + dto.getRoleId()));
            user.setRole(role);
        }

        user.setEnabled(dto.isEnabled());

        // ✅ Save and return with role initialized
        User savedUser = userRepository.save(user);

        // ✅ Force initialization of role within transaction
        if (savedUser.getRole() != null) {
            savedUser.getRole().getName(); // Initialize the proxy
        }

        return savedUser;
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found: " + id);
        }
        userRepository.deleteById(id); // HARD delete
    }

    @Override
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        if (!user.isEnabled())
            throw new RuntimeException("User is disabled (soft deleted)");
        return user;
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(User::isEnabled);
    }

    @Override
    public List<User> getUsersByName(String name) {
        return userRepository.findByFullNameContainingIgnoreCase(name)
                .stream()
                .filter(User::isEnabled)
                .toList();
    }

    @Override
    public List<User> getUsersByRole(String roleName) {
        return userRepository.findByRole_Name(roleName.toUpperCase())
                .stream()
                .filter(User::isEnabled)
                .toList();
    }

    @Override
    public List<User> getAllUsers(boolean includeDisabled) {
        if (includeDisabled) {
            return userRepository.findAllUsersWithRole();
        }
        return userRepository.findAllEnabledUsersWithRole().stream()
                .filter(User::isEnabled)
                .toList();
    }

    @Override
    public void updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        user.setRole(role);
        userRepository.save(user);
    }
}