package com.app.backend.service;

// Core Spring Framework
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Jakarta/Java EE
import jakarta.annotation.PostConstruct;

// Lombok
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Java Standard Library
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// Application-specific imports
import com.app.backend.dto.PageResponseDTO;
import com.app.backend.dto.TextEntryDTO;
import com.app.backend.dto.TextEntryResponseDTO;
import com.app.backend.entity.TextEntry;
import com.app.backend.entity.User;
import com.app.backend.repository.TextEntryRepository;
import com.app.backend.service.UserService;
import com.app.backend.util.TextEntryNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "textEntries")
public class TextEntryService {

    private final TextEntryRepository textEntryRepository;
    private final UserService userService;
    private final CacheManager cacheManager;

    @Value("${app.cache.use-auto:false}")
    private boolean useAutoCache;

    // ============= Cache Monitoring via Manual Logging =============

    @PostConstruct
    public void initializeCacheService() {
        log.info("🚀 TextEntryService initialized with cache implementation: {}",
                useAutoCache ? "AUTO (@Cacheable)" : "MANUAL");

        // Test cache manager availability
        try {
            if (cacheManager != null) {
                log.info("✅ Cache manager available: {}", cacheManager.getClass().getSimpleName());
                log.info("📊 Available caches: {}", cacheManager.getCacheNames());
            }
        } catch (Exception ex) {
            log.warn("⚠️ Cache manager initialization warning: {}", ex.getMessage());
        }
    }

    // ============= CRUD Operations with Cache Management =============

    @CachePut(value = "textEntries", key = "#result.id", condition = "#result != null")
    @CacheEvict(value = "textEntries", allEntries = true) // Clear all list caches
    @Transactional
    public TextEntryResponseDTO createTextEntry(TextEntryDTO dto, String userEmail) {
        log.info("Creating new TextEntry for user: {}", userEmail);
        validateTextEntryDTO(dto);

        User user = userService.getUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        TextEntry textEntry = mapDtoToEntity(dto, user);
        TextEntry saved = textEntryRepository.save(textEntry);
        TextEntryResponseDTO responseDTO = mapToResponseDTO(saved);

        log.info("✅ Successfully created TextEntry with id: {} for user: {}", saved.getId(), userEmail);
        log.info("🧹 Cache invalidated due to new entry creation");
        return responseDTO;
    }

    @CachePut(value = "textEntries", key = "#id", condition = "#result != null")
    @CacheEvict(value = "textEntries", allEntries = true) // Clear all list caches
    @Transactional
    public TextEntryResponseDTO updateTextEntry(Long id, TextEntryDTO dto, String userEmail) {
        log.info("Updating TextEntry with id: {} by user: {}", id, userEmail);
        validateTextEntryDTO(dto);

        TextEntry existing = findEntityById(id);
        User currentUser = userService.getUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        if (!existing.getUser().getId().equals(currentUser.getId()) && !hasAdminRole(currentUser)) {
            throw new RuntimeException("Access denied: You can only update your own text entries");
        }

        existing.setMessage(dto.getMessage());
        TextEntry updated = textEntryRepository.save(existing);

        log.info("✅ Successfully updated TextEntry with id: {}", id);
        log.info("🧹 Cache invalidated due to entry update");
        return mapToResponseDTO(updated);
    }

    @Caching(evict = {
            @CacheEvict(value = "textEntries", key = "#id"),                    // Clear specific entry
            @CacheEvict(value = "textEntries", allEntries = true)              // Clear all list caches
    })
    @Transactional
    public void deleteTextEntry(Long id, String userEmail) {
        log.info("Deleting TextEntry with id: {} by user: {}", id, userEmail);

        TextEntry existing = findEntityById(id);
        User currentUser = userService.getUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        if (!existing.getUser().getId().equals(currentUser.getId()) && !hasAdminRole(currentUser)) {
            throw new RuntimeException("Access denied: You can only delete your own text entries");
        }

        textEntryRepository.deleteById(id);
        log.info("✅ Successfully deleted TextEntry with id: {}", id);
        log.info("🧹 Cache invalidated due to entry deletion");
    }

    // ============= Hybrid Caching Implementation =============

    public TextEntryResponseDTO findByIdCached(Long id) {
        return useAutoCache ? findByIdAuto(id) : findByIdManual(id);
    }

    // Manual Cache Implementation (Your current working solution)
    @Transactional(readOnly = true)
    public TextEntryResponseDTO findByIdManual(Long id) {
        log.debug("Finding TextEntry by ID: {} using MANUAL cache", id);

        String cacheKey = "single-entry-" + id;
        Cache cache = cacheManager.getCache("textEntries");

        try {
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null) {
                log.debug("🚀 MANUAL CACHE HIT for entry {}", id);
                return (TextEntryResponseDTO) cached.get();
            }

            log.info("🔥 MANUAL CACHE MISS - Loading entry {} from database", id);
            TextEntry entity = textEntryRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new TextEntryNotFoundException("TextEntry not found with id: " + id));

            TextEntryResponseDTO result = mapToResponseDTO(entity);
            cache.put(cacheKey, result);
            log.debug("✅ Cached entry {} successfully", id);

            return result;
        } catch (Exception ex) {
            log.warn("⚠️ Cache error for entry {}, falling back to database: {}", id, ex.getMessage());
            // Fallback to database if cache fails
            TextEntry entity = textEntryRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new TextEntryNotFoundException("TextEntry not found with id: " + id));
            return mapToResponseDTO(entity);
        }
    }

    // @Cacheable Implementation
    @Cacheable(
            value = "textEntries",
            key = "'auto-entry-' + #id",
            unless = "#result == null",
            condition = "#id != null && #id > 0"
    )
    @Transactional(readOnly = true)
    public TextEntryResponseDTO findByIdAuto(Long id) {
        log.info("🚀 AUTO CACHE: Loading entry {} from database", id);

        TextEntry entity = textEntryRepository.findByIdWithUser(id)
                .orElseThrow(() -> new TextEntryNotFoundException("TextEntry not found with id: " + id));

        TextEntryResponseDTO result = mapToResponseDTO(entity);
        log.debug("✅ Auto-cached entry {} successfully", id);
        return result;
    }

    // ============= List Operations with Hybrid Caching =============

    public List<TextEntryResponseDTO> findAllTextEntriesSimple() {
        return useAutoCache ? findAllTextEntriesAuto() : findAllTextEntriesManual();
    }

    // Manual Cache Implementation for Lists
    @Transactional(readOnly = true)
    public List<TextEntryResponseDTO> findAllTextEntriesManual() {
        log.debug("Fetching all TextEntries using MANUAL cache");

        String cacheKey = "simple-data-v4";
        Cache cache = cacheManager.getCache("textEntries");

        try {
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null) {
                log.debug("🚀 MANUAL CACHE HIT for all entries");
                return (List<TextEntryResponseDTO>) cached.get();
            }

            log.info("🔥 MANUAL CACHE MISS - Loading all entries from database");
            List<TextEntry> entities = textEntryRepository.findAllWithUserSimple();
            List<TextEntryResponseDTO> result = entities.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());

            cache.put(cacheKey, result);
            log.info("✅ Cached {} entries successfully", result.size());

            return result;
        } catch (Exception ex) {
            log.warn("⚠️ Cache error for all entries, falling back to database: {}", ex.getMessage());
            // Fallback to database if cache fails
            List<TextEntry> entities = textEntryRepository.findAllWithUserSimple();
            return entities.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
        }
    }

    // @Cacheable Implementation for Lists
    @Cacheable(
            value = "textEntries",
            key = "'auto-simple-data-v4'",
            unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<TextEntryResponseDTO> findAllTextEntriesAuto() {
        log.info("🚀 AUTO CACHE: Loading all entries from database");

        List<TextEntry> entities = textEntryRepository.findAllWithUserSimple();
        List<TextEntryResponseDTO> result = entities.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        log.info("✅ Auto-cached {} entries successfully", result.size());
        return result;
    }

    // ============= Filtered Operations =============

//    @Cacheable(
//            value = "textEntries",
//            key = "'search-' + #searchTerm",
//            unless = "#result == null || #result.isEmpty()",
//            condition = "#searchTerm != null && !#searchTerm.isBlank()"
//    )
//    @Transactional(readOnly = true)
//    public List<TextEntryResponseDTO> searchInMessageCached(String searchTerm) {
//        log.info("🔥 CACHE MISS: Searching entries for term: '{}'", searchTerm);
//
//        List<TextEntry> entities = textEntryRepository.findByMessageContainingWithUserSimple(searchTerm);
//        List<TextEntryResponseDTO> result = entities.stream()
//                .map(this::mapToResponseDTO)
//                .collect(Collectors.toList());
//
//        log.info("✅ Cached {} search results for term: '{}'", result.size(), searchTerm);
//        return result;
//    }

    // ✅ Remove @Cacheable annotation - it's not working
// @Cacheable(...)  ← Remove this
    @Transactional(readOnly = true)
    public List<TextEntryResponseDTO> searchInMessageCached(String searchTerm) {
        log.debug("Searching TextEntries using MANUAL cache for term: '{}'", searchTerm);

        String cacheKey = "search-" + searchTerm;
        Cache cache = cacheManager.getCache("textEntries");

        try {
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null) {
                log.info("🚀 MANUAL CACHE HIT for search term: '{}'", searchTerm);
                return (List<TextEntryResponseDTO>) cached.get();
            }

            log.info("🔥 MANUAL CACHE MISS - Searching for term: '{}' in database", searchTerm);
            List<TextEntry> entities = textEntryRepository.findByMessageContainingWithUserSimple(searchTerm);
            List<TextEntryResponseDTO> result = entities.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());

            cache.put(cacheKey, result);
            log.info("✅ Manually cached {} search results for term: '{}'", result.size(), searchTerm);

            return result;
        } catch (Exception ex) {
            log.warn("⚠️ Cache error for search term '{}', falling back to database: {}", searchTerm, ex.getMessage());
            List<TextEntry> entities = textEntryRepository.findByMessageContainingWithUserSimple(searchTerm);
            return entities.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
        }
    }

    @Cacheable(
            value = "textEntries",
            key = "'owner-' + #owner",
            unless = "#result == null || #result.isEmpty()",
            condition = "#owner != null && !#owner.isBlank()"
    )
    @Transactional(readOnly = true)
    public List<TextEntryResponseDTO> findByOwnerCached(String owner) {
        log.info("🔥 CACHE MISS: Loading entries for owner: '{}'", owner);

        List<TextEntry> entities = textEntryRepository.findByOwnerWithUserSimple(owner);
        List<TextEntryResponseDTO> result = entities.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        log.info("✅ Cached {} entries for owner: '{}'", result.size(), owner);
        return result;
    }

    @Cacheable(
            value = "textEntries",
            key = "'daterange-' + #startDate.toString() + '-' + #endDate.toString()",
            unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<TextEntryResponseDTO> findByDateRangeCached(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("🔥 CACHE MISS: Loading entries for date range: {} to {}", startDate, endDate);

        List<TextEntry> entities = textEntryRepository.findByCreatedAtBetweenWithUserSimple(startDate, endDate);
        List<TextEntryResponseDTO> result = entities.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        log.info("✅ Cached {} entries for date range", result.size());
        return result;
    }

    @Cacheable(
            value = "textEntries",
            key = "'user-' + #userEmail",
            unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<TextEntryResponseDTO> findByUserCached(String userEmail) {
        log.info("🔥 CACHE MISS: Loading entries for user: '{}'", userEmail);

        User user = userService.getUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        List<TextEntry> entities = textEntryRepository.findByUserOrderByCreatedAtDesc(user);
        List<TextEntryResponseDTO> result = entities.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        log.info("✅ Cached {} entries for user: '{}'", result.size(), userEmail);
        return result;
    }

    // ============= Pagination Methods =============

    @Transactional(readOnly = true)
    public PageResponseDTO<TextEntryResponseDTO> findAllTextEntriesPaginated(Pageable pageable) {
        List<TextEntryResponseDTO> allEntries = findAllTextEntriesSimple();
        return createPageFromList(allEntries, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TextEntryResponseDTO> searchInMessagePaginated(String searchTerm, Pageable pageable) {
        List<TextEntryResponseDTO> allEntries = searchInMessageCached(searchTerm);
        return createPageFromList(allEntries, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TextEntryResponseDTO> findByOwnerPaginated(String owner, Pageable pageable) {
        List<TextEntryResponseDTO> allEntries = findByOwnerCached(owner);
        return createPageFromList(allEntries, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TextEntryResponseDTO> findByDateRangePaginated(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        List<TextEntryResponseDTO> allEntries = findByDateRangeCached(startDate, endDate);
        return createPageFromList(allEntries, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TextEntryResponseDTO> findByUserPaginated(String userEmail, Pageable pageable) {
        List<TextEntryResponseDTO> allEntries = findByUserCached(userEmail);
        return createPageFromList(allEntries, pageable);
    }

    // ============= Count Operations =============

    @Cacheable(value = "textEntries", key = "'total-count'")
    @Transactional(readOnly = true)
    public long getTotalCountCached() {
        log.info("🔥 CACHE MISS: Getting total count from database");
        long count = textEntryRepository.count();
        log.debug("✅ Cached total count: {}", count);
        return count;
    }

    @Cacheable(value = "textEntries", key = "'count-owner-' + #owner")
    @Transactional(readOnly = true)
    public long getCountByOwnerCached(String owner) {
        log.info("🔥 CACHE MISS: Getting count for owner '{}' from database", owner);
        long count = textEntryRepository.countByOwnerIgnoreCase(owner);
        log.debug("✅ Cached count for owner '{}': {}", owner, count);
        return count;
    }

    // ============= Cache Management Operations =============

    public Map<String, Object> performCacheOperation(boolean clearAll, String cacheName, boolean warmUp) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (clearAll) {
                clearAllCaches();
                result.put("operation", "cleared_all_caches");
                log.info("🧹 Performed: Clear ALL caches");
            } else {
                clearCacheByName(cacheName);
                result.put("operation", "cleared_cache");
                result.put("cacheName", cacheName);
                log.info("🧹 Performed: Clear cache '{}'", cacheName);
            }

            if (warmUp) {
                warmUpCache();
                result.put("warmUp", "completed");
                log.info("🔥 Performed: Cache warm-up");
            }

            result.put("statistics", getCacheStatistics());
            result.put("success", true);

        } catch (Exception ex) {
            log.error("❌ Cache operation failed", ex);
            result.put("success", false);
            result.put("error", ex.getMessage());
        }

        return result;
    }

    @CacheEvict(value = "textEntries", allEntries = true)
    public void clearAllCaches() {
        log.warn("🧹 Clearing ALL textEntry caches");
    }

    public void clearCacheByName(String cacheName) {
        try {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).clear();
                log.info("🧹 Successfully cleared cache: {}", cacheName);
            } else {
                throw new IllegalArgumentException("Cache not found: " + cacheName);
            }
        } catch (Exception ex) {
            log.error("❌ Failed to clear cache: {}", cacheName, ex);
            throw new RuntimeException("Failed to clear cache: " + cacheName, ex);
        }
    }

    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            stats.put("totalCaches", cacheNames.size());
            stats.put("cacheNames", cacheNames);
            stats.put("cacheImplementation", useAutoCache ? "AUTO (@Cacheable)" : "MANUAL");

            if (cacheManager instanceof RedisCacheManager) {
                stats.put("cacheType", "Redis");
            }

            // Add cache key statistics
            stats.put("estimatedKeys", getEstimatedCacheKeys());
            stats.put("configuredAutoCache", useAutoCache);

        } catch (Exception ex) {
            log.error("❌ Failed to get cache statistics", ex);
            stats.put("error", ex.getMessage());
        }

        return stats;
    }

    public Map<String, Object> toggleCacheImplementation(boolean useAutoCache) {
        String previousImpl = this.useAutoCache ? "AUTO" : "MANUAL";
        this.useAutoCache = useAutoCache;
        String currentImpl = useAutoCache ? "AUTO" : "MANUAL";

        Map<String, Object> result = new HashMap<>();
        result.put("previousImplementation", previousImpl);
        result.put("currentImplementation", currentImpl);
        result.put("description", useAutoCache ?
                "Using @Cacheable annotations with automatic cache management" :
                "Using manual cache management with explicit control");

        log.info("🔄 Cache implementation toggled from {} to {}", previousImpl, currentImpl);

        return result;
    }

    public void warmUpCache() {
        try {
            log.info("🔥 Starting cache warm-up process");

            // Warm up main list
            List<TextEntryResponseDTO> allEntries = findAllTextEntriesSimple();
            log.info("📊 Warmed up main list cache with {} entries", allEntries.size());

            // Warm up total count
            long totalCount = getTotalCountCached();
            log.info("📊 Warmed up count cache: {} total entries", totalCount);

            log.info("✅ Cache warm-up completed successfully");

        } catch (Exception ex) {
            log.error("❌ Cache warm-up failed", ex);
        }
    }

    // ============= Helper Methods =============

    private TextEntry findEntityById(Long id) {
        return textEntryRepository.findByIdWithUser(id)
                .orElseThrow(() -> new TextEntryNotFoundException("TextEntry not found with id: " + id));
    }

    private PageResponseDTO<TextEntryResponseDTO> createPageFromList(List<TextEntryResponseDTO> allEntries, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allEntries.size());

        List<TextEntryResponseDTO> pageContent = start < allEntries.size()
                ? allEntries.subList(start, end)
                : new ArrayList<>();

        PageResponseDTO<TextEntryResponseDTO> result = new PageResponseDTO<>();
        result.setContent(pageContent);
        result.setPageNumber(pageable.getPageNumber());
        result.setPageSize(pageable.getPageSize());
        result.setTotalElements(allEntries.size());
        result.setTotalPages((int) Math.ceil((double) allEntries.size() / pageable.getPageSize()));
        result.setFirst(pageable.getPageNumber() == 0);
        result.setLast(pageable.getPageNumber() >= result.getTotalPages() - 1);
        result.setEmpty(pageContent.isEmpty());
        result.setNumberOfElements(pageContent.size());

        return result;
    }

    private int getEstimatedCacheKeys() {
        // This is an estimation - in production you might want more accurate counting
        return 10; // Estimated based on typical usage patterns
    }

    private TextEntryResponseDTO mapToResponseDTO(TextEntry textEntry) {
        return TextEntryResponseDTO.builder()
                .id(textEntry.getId())
                .message(textEntry.getMessage())
                .owner(textEntry.getOwner())
                .user(TextEntryResponseDTO.UserBasicDTO.builder()
                        .id(textEntry.getUser().getId())
                        .fullName(textEntry.getUser().getFullName())
                        .build())
                .createdAt(textEntry.getCreatedAt())
                .updatedAt(textEntry.getUpdatedAt())
                .build();
    }

    private void validateTextEntryDTO(TextEntryDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("TextEntryDTO cannot be null");
        }
        if (dto.getMessage() != null && dto.getMessage().trim().length() > 2048) {
            throw new IllegalArgumentException("Message exceeds maximum length of 2048 characters");
        }
    }

    private TextEntry mapDtoToEntity(TextEntryDTO dto, User user) {
        TextEntry entry = new TextEntry();
        entry.setMessage(dto.getMessage().trim());
        entry.setOwner(user.getFullName());
        entry.setUser(user);
        return entry;
    }

    private boolean hasAdminRole(User user) {
        return user.getRole().getName().equals("ADMIN");
    }
}