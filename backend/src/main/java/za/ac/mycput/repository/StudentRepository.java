package za.ac.mycput.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.mycput.domain.Student;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    @EntityGraph(attributePaths = "user")
    Optional<Student> findByUser_UserId(Integer userId);

    List<Student> findByUser_UserIdIn(Collection<Integer> userIds);

    boolean existsByStudentNumberIgnoreCase(String studentNumber);

    /** Students who want to hear about new matching jobs (with their user loaded, for notifications). */
    @EntityGraph(attributePaths = "user")
    List<Student> findByJobAlertsTrue();
}
