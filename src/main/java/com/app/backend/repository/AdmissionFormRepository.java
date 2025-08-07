package com.app.backend.repository;

import com.app.backend.entity.AdmissionForm;
import com.app.backend.entity.AdmissionGender;
import com.app.backend.entity.ApplicationStatus;
import com.app.backend.entity.ClassLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface AdmissionFormRepository extends JpaRepository<AdmissionForm, Long>, JpaSpecificationExecutor<AdmissionForm> {

    long countByApplicationStatus(ApplicationStatus status);
    long countByClassApplied(ClassLevel classLevel);
    long countByGender(AdmissionGender gender);
    long countBySubmittedAtGreaterThanEqual(LocalDateTime date);

    Optional<AdmissionForm> findByApplicationNumber(String applicationNumber);

    boolean existsByEmailAddress(String emailAddress);

    boolean existsByContactNumber(String contactNumber);

    boolean existsByEmailAddressAndIdNot(String emailAddress, Long id);

    boolean existsByContactNumberAndIdNot(String contactNumber, Long id);

    List<AdmissionForm> findByApplicationStatus(ApplicationStatus status);

    Page<AdmissionForm> findByApplicationStatus(ApplicationStatus status, Pageable pageable);

    @Query("SELECT af FROM AdmissionForm af WHERE af.submittedAt BETWEEN :startDate AND :endDate")
    List<AdmissionForm> findBySubmittedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    @Query("SELECT af FROM AdmissionForm af WHERE af.studentName LIKE %:name%")
    List<AdmissionForm> findByStudentNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT COUNT(af) FROM AdmissionForm af WHERE DATE(af.submittedAt) = :date")
    long countBySubmissionDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(af) FROM AdmissionForm af WHERE af.applicationStatus = :status")
    long countByStatus(@Param("status") ApplicationStatus status);






    @Query(value = "SELECT TO_CHAR(submitted_at, 'YYYY-MM') as month, COUNT(*) as count " +
            "FROM academy_admission_form WHERE submitted_at >= :since " +
            "GROUP BY TO_CHAR(submitted_at, 'YYYY-MM') " +
            "ORDER BY month", nativeQuery = true)
    List<Object[]> findMonthlyApplicationCountsRaw(@Param("since") LocalDateTime since);

    default Map<String, Long> findMonthlyApplicationCounts(LocalDateTime since) {
        List<Object[]> results = findMonthlyApplicationCountsRaw(since);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (updated_at - submitted_at))/3600.0) " +
            "FROM academy_admission_form WHERE updated_at > submitted_at", nativeQuery = true)
    Double calculateAverageProcessingTimeRaw();

    default double calculateAverageProcessingTime() {
        Double result = calculateAverageProcessingTimeRaw();
        return result != null ? result : 0.0;
    }

}
