package za.ac.mycput.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.mycput.domain.Interview;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Integer> {

    /** Interviews (with their slots) for a batch of applications — used when listing applications. */
    @Query("select distinct i from Interview i left join fetch i.slots where i.application.applicationId in :ids")
    List<Interview> findByApplicationIds(@Param("ids") Collection<Integer> applicationIds);

    @Query("select i from Interview i where i.application.applicationId = :applicationId")
    Optional<Interview> findByApplicationId(@Param("applicationId") Integer applicationId);

    /** One interview with everything needed for ownership checks, notifications and calendar files. */
    @Query("""
            select distinct i from Interview i
            left join fetch i.slots
            join fetch i.application a
            join fetch a.job j join fetch j.company c join fetch c.user
            join fetch a.student s join fetch s.user
            where i.interviewId = :id
            """)
    Optional<Interview> findWithDetails(@Param("id") Integer interviewId);

    @Query("""
            select distinct i from Interview i
            left join fetch i.slots
            join fetch i.application a
            join fetch a.job j join fetch j.company c
            join fetch a.student s
            where s.user.userId = :userId and i.status <> za.ac.mycput.domain.enums.InterviewStatus.CANCELLED
            """)
    List<Interview> findActiveForStudentUser(@Param("userId") Integer userId);

    @Query("""
            select distinct i from Interview i
            left join fetch i.slots
            join fetch i.application a
            join fetch a.job j join fetch j.company c
            join fetch a.student s
            where c.user.userId = :userId and i.status <> za.ac.mycput.domain.enums.InterviewStatus.CANCELLED
            """)
    List<Interview> findActiveForCompanyUser(@Param("userId") Integer userId);

    @Modifying
    @Query("delete from InterviewSlot s where s.interview.interviewId in "
            + "(select i.interviewId from Interview i where i.application.job.jobId = :jobId)")
    int deleteSlotsByJobId(@Param("jobId") Integer jobId);

    @Modifying
    @Query("delete from Interview i where i.application.applicationId in "
            + "(select a.applicationId from Application a where a.job.jobId = :jobId)")
    int deleteByJobId(@Param("jobId") Integer jobId);
}
