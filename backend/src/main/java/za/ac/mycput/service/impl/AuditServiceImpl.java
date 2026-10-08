package za.ac.mycput.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import za.ac.mycput.domain.AuditLog;
import za.ac.mycput.domain.enums.AuditAction;
import za.ac.mycput.dto.AdminDtos.AuditLogResponse;
import za.ac.mycput.dto.CommonDtos.PageResponse;
import za.ac.mycput.repository.AuditLogRepository;
import za.ac.mycput.service.IAuditService;
import za.ac.mycput.service.support.DtoMapper;

@Service
public class AuditServiceImpl implements IAuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate newTransaction;

    public AuditServiceImpl(AuditLogRepository auditLogRepository, PlatformTransactionManager transactionManager) {
        this.auditLogRepository = auditLogRepository;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Writes the entry in its own transaction (REQUIRES_NEW), so it is kept even when the surrounding
     * action fails and rolls back — e.g. a failed login must still be recorded.
     * Auditing is best-effort: if the entry cannot be written, the error is logged and the user's
     * action still succeeds. The try/catch wraps the whole transaction, including its commit.
     */
    @Override
    public void record(Integer actorUserId, String actorEmail, AuditAction action,
                       String targetType, Integer targetId, String details) {
        AuditLog entry = new AuditLog(actorUserId, truncate(actorEmail, 100), action, targetType,
                targetId, truncate(details, 500), currentIp());
        try {
            newTransaction.executeWithoutResult(status -> auditLogRepository.save(entry));
        } catch (RuntimeException e) {
            log.warn("Could not write audit log entry {} for {}", action, actorEmail, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditAction action, String actorEmail, int page, int size) {
        String pattern = (actorEmail == null || actorEmail.isBlank()) ? null : "%" + actorEmail.trim().toLowerCase() + "%";
        var results = auditLogRepository.search(action, pattern,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("logId"))));
        return PageResponse.of(results, DtoMapper::toAuditLogResponse);
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getRemoteAddr();
            // Show the IPv6 loopback address in its usual short form
            return "0:0:0:0:0:0:0:1".equals(ip) ? "::1" : ip;
        }
        return null;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
