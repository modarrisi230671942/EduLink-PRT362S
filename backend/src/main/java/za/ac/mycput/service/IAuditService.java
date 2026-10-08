package za.ac.mycput.service;

import za.ac.mycput.domain.enums.AuditAction;
import za.ac.mycput.dto.AdminDtos.AuditLogResponse;
import za.ac.mycput.dto.CommonDtos.PageResponse;

public interface IAuditService {

    /**
     * Records an event. Never throws: a logging failure must not break the action being logged.
     * @param actorUserId null for anonymous events
     */
    void record(Integer actorUserId, String actorEmail, AuditAction action,
                String targetType, Integer targetId, String details);

    PageResponse<AuditLogResponse> search(AuditAction action, String actorEmail, int page, int size);
}
