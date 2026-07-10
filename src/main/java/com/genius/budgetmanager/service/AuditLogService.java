package com.genius.budgetmanager.service;

import com.genius.budgetmanager.model.AuditLog;
import com.genius.budgetmanager.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository repository;

    public AuditLog record(String entityType, Long entityId, String action,
                           String user, Map<String, Object> beforeState,
                           Map<String, Object> afterState) {
        Map<String, Map<String, Object>> changes =
            (beforeState != null)
                ? AuditLog.computeChanges(beforeState, afterState)
                : Map.of();

        AuditLog entry = new AuditLog();
        entry.setTimestamp(Instant.now().toString());
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setUser(user != null ? user : "unknown");
        entry.setBeforeState(beforeState);
        entry.setAfterState(afterState);
        entry.setChanges(changes);

        return repository.save(entry);
    }

    public List<AuditLog> getHistory(String entityType, String action,
                                      String dateFrom, String dateTo,
                                      String user, Long entityId) {
        return repository.findFiltered(entityType, action, dateFrom, dateTo, user, entityId);
    }
}
