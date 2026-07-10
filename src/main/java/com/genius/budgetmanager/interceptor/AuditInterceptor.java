package com.genius.budgetmanager.interceptor;

import com.genius.budgetmanager.model.AuditLog;
import com.genius.budgetmanager.model.Campaign;
import com.genius.budgetmanager.service.AuditLogService;
import com.genius.budgetmanager.service.CampaignService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class AuditInterceptor implements HandlerInterceptor {

    public static final ThreadLocal<String> CurrentUser = new ThreadLocal<>();

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CampaignService campaignService;

    private final ThreadLocal<Map<String, Object>> beforeSnapshot = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;

        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (!isMutatingCampaignRequest(method, uri)) return true;

        CurrentUser.set(resolveUser(request));

        Long entityId = extractEntityId(uri);
        if (entityId != null) {
            try {
                Campaign existing = campaignService.getCampaignById(entityId);
                beforeSnapshot.set(AuditLog.campaignToMap(existing));
            } catch (Exception e) {
                beforeSnapshot.set(null);
            }
        } else {
            beforeSnapshot.set(null);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod)) return;

        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (!isMutatingCampaignRequest(method, uri)) return;
        if (response.getStatus() >= 400) {
            beforeSnapshot.remove();
            CurrentUser.remove();
            return;
        }

        Long entityId = extractEntityId(uri);
        Map<String, Object> before = beforeSnapshot.get();
        beforeSnapshot.remove();

        String user = CurrentUser.get();
        CurrentUser.remove();

        try {
            String action = resolveAction(method, uri);

            if (entityId != null && before != null) {
                Campaign updated = campaignService.getCampaignById(entityId);
                Map<String, Object> after = AuditLog.campaignToMap(updated);
                auditLogService.record("campaign", entityId, action, user, before, after);
            }
        } catch (Exception e) {
            System.err.println("Audit log error: " + e.getMessage());
        }
    }

    private boolean isMutatingCampaignRequest(String method, String uri) {
        if (!uri.startsWith("/api/campaigns")) return false;
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private Long extractEntityId(String uri) {
        String[] parts = uri.replace("/api/campaigns/", "").split("/");
        try {
            return Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveAction(String method, String uri) {
        if ("POST".equals(method)) return "create";
        if (uri.contains("/expenses")) return "expense_added";
        if (uri.contains("/status")) return "status_change";
        return "update";
    }

    private String resolveUser(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) ip = "127.0.0.1";
        int port = request.getRemotePort();
        return ip + ":" + port;
    }
}
