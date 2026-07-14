package com.genius.budgetmanager.repository;

import com.genius.budgetmanager.model.AuditLog;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class AuditLogRepository {

    private static final int MAX_ENTRIES = 1000;

    private final List<AuditLog> logs = new ArrayList<>();
    private long nextId = 1;

    public AuditLog save(AuditLog entry) {
        entry.setId(nextId++);
        logs.add(entry);
        if (logs.size() > MAX_ENTRIES) {
            logs.remove(0);
        }
        return entry;
    }

    public List<AuditLog> findAll() {
        return new ArrayList<>(logs);
    }

    public List<AuditLog> findFiltered(String entityType, String action,
                                        String dateFrom, String dateTo,
                                        String user, Long entityId) {
        return logs.stream()
            .filter(e -> entityType == null || entityType.equalsIgnoreCase(e.getEntityType()))
            .filter(e -> action == null || action.equalsIgnoreCase(e.getAction()))
            .filter(e -> user == null || matchesUserFilter(e.getUser(), user))
            .filter(e -> entityId == null || entityId.equals(e.getEntityId()))
            .filter(e -> dateFrom == null || (e.getTimestamp() != null && e.getTimestamp().compareTo(dateFrom) >= 0))
            .filter(e -> dateTo == null || (e.getTimestamp() != null && e.getTimestamp().compareTo(dateTo) <= 0))
            .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
            .collect(Collectors.toList());
    }

    private boolean matchesUserFilter(String storedUser, String filter) {
        if (storedUser == null) return false;
        if (filter.equals("*")) return true;

        String[] storedParts = storedUser.split(":");
        String storedIp = storedParts.length > 0 ? storedParts[0] : "";
        String storedPort = storedParts.length > 1 ? storedParts[1] : "";

        if (filter.contains(":")) {
            String[] filterParts = filter.split(":");
            String filterIp = filterParts.length > 0 ? filterParts[0] : "";
            String filterPort = filterParts.length > 1 ? filterParts[1] : "";

            if (!filterIp.isEmpty() && !filterPort.isEmpty()) {
                return storedIp.equals(filterIp) && storedPort.equals(filterPort);
            }
            if (!filterIp.isEmpty()) return storedIp.equals(filterIp);
            if (!filterPort.isEmpty()) return storedPort.equals(filterPort);
        }

        return storedIp.contains(filter) || storedPort.contains(filter);
    }
}
