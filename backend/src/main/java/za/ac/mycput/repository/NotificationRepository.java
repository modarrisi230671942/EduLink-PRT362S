package za.ac.mycput.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.mycput.domain.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findTop20ByUser_UserIdOrderByCreatedAtDescNotificationIdDesc(Integer userId);

    long countByUser_UserIdAndIsReadFalse(Integer userId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.user.userId = :userId and n.isRead = false")
    int markAllRead(@Param("userId") Integer userId);
}
