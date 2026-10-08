package za.ac.mycput.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.mycput.domain.Application;
import za.ac.mycput.domain.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    boolean existsByJob_JobIdAndStudent_StudentId(Integer jobId, Integer studentId);

    long countByStatus(ApplicationStatus status);

    /** A student's own applications, newest first. */
    @Query("""
            select a from Application a
            join fetch a.job j join fetch j.company
            join fetch a.student s
            where s.studentId = :studentId
            order by a.appliedDate desc
            """)
    List<Application> findByStudentId(@Param("studentId") Integer studentId);

    /** Applications to a company's jobs; status and job filters are optional. */
    @Query("""
            select a from Application a
            join fetch a.job j join fetch j.company c
            join fetch a.student s join fetch s.user
            where c.companyId = :companyId
              and (:status is null or a.status = :status)
              and (:jobId is null or j.jobId = :jobId)
            order by a.appliedDate desc
            """)
    List<Application> findForCompany(@Param("companyId") Integer companyId,
                                     @Param("status") ApplicationStatus status,
                                     @Param("jobId") Integer jobId);

    /** One application with everything needed for ownership checks and notifications. */
    @Query("""
            select a from Application a
            join fetch a.job j join fetch j.company c join fetch c.user
            join fetch a.student s join fetch s.user
            where a.applicationId = :applicationId
            """)
    Optional<Application> findWithDetails(@Param("applicationId") Integer applicationId);

    @Modifying
    @Query("delete from Application a where a.job.jobId = :jobId")
    int deleteByJobId(@Param("jobId") Integer jobId);

    @Query("select a.job.jobId from Application a where a.student.studentId = :studentId")
    List<Integer> findJobIdsByStudentId(@Param("studentId") Integer studentId);

    /** Rows of [jobId, count] for a company's jobs. */
    @Query("select a.job.jobId, count(a) from Application a where a.job.company.companyId = :companyId group by a.job.jobId")
    List<Object[]> countPerJobForCompany(@Param("companyId") Integer companyId);

    // ── Admin analytics ─────────────────────────────────────────────────────

    @Query("select a.appliedDate from Application a where a.appliedDate >= :since")
    List<LocalDateTime> findAppliedDatesSince(@Param("since") LocalDateTime since);

    /** Rows of [ApplicationStatus, count]. */
    @Query("select a.status, count(a) from Application a group by a.status")
    List<Object[]> countByStatusGrouped();

    /** Rows of [companyName, count], most applications first. */
    @Query("""
            select c.companyName, count(a) from Application a
            join a.job j join j.company c
            group by c.companyId, c.companyName
            order by count(a) desc
            """)
    List<Object[]> countPerCompany(Pageable pageable);
}
