package za.ac.mycput.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.mycput.domain.User;
import za.ac.mycput.domain.enums.UserType;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByUserType(UserType userType);

    /** Admin user list. Both filters are optional; {@code emailPattern} is a lowercase LIKE pattern. */
    @Query("""
            select u from User u
            where (:role is null or u.userType = :role)
              and (:emailPattern is null or lower(u.email) like :emailPattern)
            """)
    Page<User> search(@Param("role") UserType role,
                      @Param("emailPattern") String emailPattern,
                      Pageable pageable);
}
