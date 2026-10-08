package za.ac.mycput.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.mycput.domain.Company;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {

    @EntityGraph(attributePaths = "user")
    Optional<Company> findByUser_UserId(Integer userId);

    List<Company> findByUser_UserIdIn(Collection<Integer> userIds);

    @EntityGraph(attributePaths = "user")
    List<Company> findAllBy(Sort sort);

    @EntityGraph(attributePaths = "user")
    List<Company> findByIsVerified(Boolean isVerified, Sort sort);

    long countByIsVerified(Boolean isVerified);
}
