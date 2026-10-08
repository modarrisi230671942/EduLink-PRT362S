package za.ac.mycput.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.mycput.domain.SavedJob;

import java.util.List;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, SavedJob.Key> {

    @Query("""
            select sj from SavedJob sj join fetch sj.job j join fetch j.company
            where sj.student.studentId = :studentId
            order by sj.savedAt desc
            """)
    List<SavedJob> findForStudent(@Param("studentId") Integer studentId);

    @Query("select sj.job.jobId from SavedJob sj where sj.student.studentId = :studentId")
    List<Integer> findJobIdsByStudentId(@Param("studentId") Integer studentId);

    @Modifying
    @Query("delete from SavedJob sj where sj.job.jobId = :jobId")
    int deleteByJobId(@Param("jobId") Integer jobId);
}
