package com.genius.budgetmanager.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AuditLog {

    private Long id;
    private String timestamp;
    private String entityType;
    private Long entityId;
    private String action;
    private String user;
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;
    private Map<String, Map<String, Object>> changes;

    public AuditLog() {}

    public AuditLog(Long id, String timestamp, String entityType, Long entityId,
                    String action, String user, Map<String, Object> beforeState,
                    Map<String, Object> afterState, Map<String, Map<String, Object>> changes) {
        this.id = id;
        this.timestamp = timestamp;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.user = user;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.changes = changes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public Map<String, Object> getBeforeState() { return beforeState; }
    public void setBeforeState(Map<String, Object> beforeState) { this.beforeState = beforeState; }

    public Map<String, Object> getAfterState() { return afterState; }
    public void setAfterState(Map<String, Object> afterState) { this.afterState = afterState; }

    public Map<String, Map<String, Object>> getChanges() { return changes; }
    public void setChanges(Map<String, Map<String, Object>> changes) { this.changes = changes; }

    public static Map<String, Object> campaignToMap(Campaign c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("client", c.getClient());
        map.put("type", c.getType());
        map.put("status", c.getStatus());
        map.put("budget", c.getBudget());
        map.put("spent", c.getSpent());
        map.put("currency", c.getCurrency());
        map.put("startDate", c.getStartDate());
        map.put("endDate", c.getEndDate());
        return map;
    }

    public static Map<String, Map<String, Object>> computeChanges(
            Map<String, Object> before, Map<String, Object> after) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if (before == null || after == null) return result;

        java.util.Set<String> allKeys = new java.util.LinkedHashSet<>();
        allKeys.addAll(before.keySet());
        allKeys.addAll(after.keySet());

        for (String key : allKeys) {
            Object bVal = before.get(key);
            Object aVal = after.get(key);
            if (!Objects.equals(bVal, aVal)) {
                Map<String, Object> diff = new LinkedHashMap<>();
                diff.put("before", bVal);
                diff.put("after", aVal);
                result.put(key, diff);
            }
        }
        return result;
    }
}
