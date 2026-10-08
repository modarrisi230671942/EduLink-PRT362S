package za.ac.mycput.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.mycput.domain.JobPosting;
import za.ac.mycput.domain.enums.JobType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer> {

    /**
     * Public job search: only jobs that are active, before their deadline, and from a verified company.
     * {@code pattern} is a lowercase LIKE pattern matched against title, requirements, company and location.
     */
    @Query(value = """
            select j from JobPosting j join fetch j.company c
            where j.isActive = true and c.isVerified = true
              and (j.applicationDeadline is null or j.applicationDeadline >= :today)
              and (:type is null or j.jobType = :type)
              and (:pattern is null
                   or lower(j.title) like :pattern
                   or lower(j.requirements) like :pattern
                   or lower(c.companyName) like :pattern
                   or lower(j.location) like :pattern)
            """,
           countQuery = """
            select count(j) from JobPosting j join j.company c
            where j.isActive = true and c.isVerified = true
              and (j.applicationDeadline is null or j.applicationDeadline >= :today)
              and (:type is null or j.jobType = :type)
              and (:pattern is null
                   or lower(j.title) like :pattern
                   or lower(j.requirements) like :pattern
                   or lower(c.companyName) like :pattern
                   or lower(j.location) like :pattern)
            """)
    Page<JobPosting> searchOpenJobs(@Param("today") LocalDate today,
                                    @Param("type") JobType type,
                                    @Param("pattern") String pattern,
                                    Pageable pageable);

    /** All open jobs (used to compute personalised recommendations). */
    @Query("""
            select j from JobPosting j join fetch j.company c
            where j.isActive = true and c.isVerified = true
              and (j.applicationDeadline is null or j.applicationDeadline >= :today)
            """)
    List<JobPosting> findOpenJobs(@Param("today") LocalDate today);

    @Query("select j from JobPosting j join fetch j.company c join fetch c.user where j.jobId = :jobId")
    Optional<JobPosting> findWithCompany(@Param("jobId") Integer jobId);

    @Query("select j from JobPosting j join fetch j.company where j.company.companyId = :companyId order by j.postedDate desc")
    List<JobPosting> findByCompanyId(@Param("companyId") Integer companyId);

    @Query("""
            select count(j) from JobPosting j join j.company c
            where j.isActive = true and c.isVerified = true
              and (j.applicationDeadline is null or j.applicationDeadline >= :today)
            """)
    long countOpenJobs(@Param("today") LocalDate today);

    /** Rows of [JobType, count] for the admin analytics chart. */
    @Query("select j.jobType, count(j) from JobPosting j group by j.jobType")
    List<Object[]> countByJobType();
}
