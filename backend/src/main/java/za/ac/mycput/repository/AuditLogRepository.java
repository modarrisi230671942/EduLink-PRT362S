package za.ac.mycput.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.mycput.domain.AuditLog;
import za.ac.mycput.domain.enums.AuditAction;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    @Query("""
            select l from AuditLog l
            where (:action is null or l.action = :action)
              and (:emailPattern is null or lower(l.actorEmail) like :emailPattern)
            """)
    Page<AuditLog> search(@Param("action") AuditAction action,
                          @Param("emailPattern") String emailPattern,
                          Pageable pageable);
}
