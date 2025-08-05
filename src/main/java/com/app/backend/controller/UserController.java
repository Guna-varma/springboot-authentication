package com.app.backend.controller;

import com.app.backend.dto.UserRequestDTO;
import com.app.backend.dto.UserResponseDTO;
import com.app.backend.dto.UserRoleUpdateRequest;
import com.app.backend.entity.Role;
import com.app.backend.entity.User;
import com.app.backend.repository.RoleRepository;
import com.app.backend.service.RateLimitService;
import com.app.backend.service.UserService;
import com.app.backend.util.WebUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final RoleRepository roleRepository;

    /**
     * Helper to apply rate limit to sensitive endpoints
     */
    private boolean isRateLimited(HttpServletRequest request, HttpHeaders headers) {
        String clientIp = WebUtils.getClientIpAddress(request);
        if (!rateLimitService.isAllowed(clientIp)) {
            headers.add("X-RateLimit-Remaining", "0");
            headers.add("Retry-After", "60");
            return true;
        }
        headers.add("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)));
        return false;
    }

    // --- ADMIN Endpoints ---

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(
            @RequestParam(name = "all", defaultValue = "false") boolean includeDisabled
    ) {
        // Soft-deletion logic: Return only enabled users unless 'all' is set to true by admin
        List<UserResponseDTO> users = userService.getAllUsers(includeDisabled)
                .stream().map(this::mapToUserDTO).toList();
        if (users.isEmpty()) throw new RuntimeException("No users found in the system.");
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/role")
    public ResponseEntity<String> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        HttpHeaders headers = new HttpHeaders();
        if (isRateLimited(httpRequest, headers))
            return ResponseEntity.status(429).headers(headers).body("Rate limit exceeded");

        userService.updateUserRole(userId, request.getRoleName());
        return ResponseEntity.ok().headers(headers).body("User role updated successfully.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO dto,
            HttpServletRequest httpRequest
    ) {
        HttpHeaders headers = new HttpHeaders();
        if (isRateLimited(httpRequest, headers))
            return ResponseEntity.status(429).headers(headers).build();

        User updatedUser = userService.updateUser(id, dto);
        return ResponseEntity.ok().headers(headers).body(mapToUserDTO(updatedUser));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        HttpHeaders headers = new HttpHeaders();
        if (isRateLimited(httpRequest, headers))
            return ResponseEntity.status(429).headers(headers).body("Rate limit exceeded");
        // Soft delete (enabled=false)
        userService.disableUser(id);
        return ResponseEntity.ok().headers(headers).body("User disabled (soft deleted)");
    }

    /**
     * WARNING: Only returns if user is enabled!
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapToUserDTO(user));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
    @GetMapping("/search/name")
    public ResponseEntity<List<UserResponseDTO>> findByName(@RequestParam String name) {
        List<UserResponseDTO> res = userService.getUsersByName(name)
                .stream().map(this::mapToUserDTO).toList();
        return ResponseEntity.ok(res);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search/email")
    public ResponseEntity<UserResponseDTO> findByEmail(@RequestParam String email) {
        Optional<User> ou = userService.getUserByEmail(email);
        return ou.map(user -> ResponseEntity.ok(mapToUserDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search/role")
    public ResponseEntity<List<UserResponseDTO>> findByRole(@RequestParam String role) {
        List<UserResponseDTO> res = userService.getUsersByRole(role)
                .stream().map(this::mapToUserDTO).toList();
        return ResponseEntity.ok(res);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles(HttpServletRequest request) {
        // ✅ Rate limiting
        String clientIp = WebUtils.getClientIpAddress(request);
        if (!rateLimitService.isAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Remaining", "0")
                    .header("Retry-After", "60")
                    .build();
        }

        try {
            List<Role> roles = roleRepository.findAll();
            return ResponseEntity.ok()
                    .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                    .body(roles);
        } catch (Exception e) {
            log.error("Failed to fetch roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                    .build();
        }
    }


    // --- Current Authenticated User ---

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication auth) {
        String email = auth.getName();
        User user = userService.getUserByEmail(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new RuntimeException("User not found or disabled"));
        return ResponseEntity.ok(mapToUserDTO(user));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/updatePersonalInfo")
    public ResponseEntity<UserResponseDTO> updateCurrentUser(
            Authentication auth,
            @Valid @RequestBody UserRequestDTO dto,
            HttpServletRequest httpRequest
    ) {
        HttpHeaders headers = new HttpHeaders();
        if (isRateLimited(httpRequest, headers))
            return ResponseEntity.status(429).headers(headers).build();

        String email = auth.getName();
        User user = userService.getUserByEmail(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new RuntimeException("User not found or disabled"));
        User updatedUser = userService.updateUser(user.getId(), dto);
        return ResponseEntity.ok().headers(headers).body(mapToUserDTO(updatedUser));
    }

    // --- DTO single mapper ---
    private UserResponseDTO mapToUserDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .gender(user.getGender() != null ? user.getGender().name() : "NOT_SPECIFIED")
                .phoneNumber(user.getPhoneNumber())
                .role(new UserResponseDTO.RoleDTO(
                        user.getRole().getId(),
                        user.getRole().getName()
                ))
                .enabled(user.isEnabled())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}