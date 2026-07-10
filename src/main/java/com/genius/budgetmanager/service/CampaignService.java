package com.genius.budgetmanager.service;

import com.genius.budgetmanager.interceptor.AuditInterceptor;
import com.genius.budgetmanager.model.AuditLog;
import com.genius.budgetmanager.model.BudgetSummary;
import com.genius.budgetmanager.model.Campaign;
import com.genius.budgetmanager.model.Expense;
import com.genius.budgetmanager.model.GlobalBudgetSummary;
import com.genius.budgetmanager.repository.CampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    @Autowired
    private CampaignRepository repository;

    @Autowired
    private AuditLogService auditLogService;

    // Fix BM-F01/BM-F02: soportar filtros por status y cliente en el listado.
    public List<Campaign> getCampaigns(String status, String client) {
        final String normalizedStatus = normalize(status);
        final String normalizedClient = normalize(client);

        List<Campaign> results = repository.findAll().stream()
            .filter(c -> normalizedStatus == null || normalizedStatus.equals(normalize(c.getStatus())))
            .filter(c -> normalizedClient == null || normalizedClient.equals(normalize(c.getClient())))
                .collect(Collectors.toList());

        if (normalizedClient != null) {
            log.info("[VIS-10] GET /campaigns?client={} → Filtro aplicado | {} campaña(s) encontradas para '{}'",
                    client, results.size(), client);
        }
        return results;
    }

    // Fix BM-F01: habilitar alta de nuevas campanas desde API.
    public Campaign createCampaign(Campaign campaign) {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign payload is required");
        }
        if (campaign.getName() == null || campaign.getName().isBlank()) {
            throw new IllegalArgumentException("Campaign name is required");
        }
        if (campaign.getClient() == null || campaign.getClient().isBlank()) {
            throw new IllegalArgumentException("Campaign client is required");
        }
        if (campaign.getBudget() == null || campaign.getBudget() < 0) {
            throw new IllegalArgumentException("Campaign budget must be >= 0");
        }

        if (campaign.getStatus() == null || campaign.getStatus().isBlank()) {
            campaign.setStatus("draft");
        }
        if (campaign.getSpent() == null) {
            campaign.setSpent(0.0);
        }
        if (campaign.getCurrency() == null || campaign.getCurrency().isBlank()) {
            campaign.setCurrency("ARS");
        }

        Campaign created = repository.saveCampaign(campaign);
        log.info("[VIS-09] POST /campaigns → Campaña creada | id={} name='{}' client='{}' budget={} status={}",
                created.getId(), created.getName(), created.getClient(), created.getBudget(), created.getStatus());
        return created;
    }

    public Campaign getCampaignById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
    }

    public BudgetSummary getBudgetSummary(Long campaignId) {
        Campaign campaign = getCampaignById(campaignId);

        BudgetSummary summary = new BudgetSummary();
        summary.setCampaignId(campaign.getId());
        summary.setCampaignName(campaign.getName());
        summary.setClient(campaign.getClient());
        summary.setTotalBudget(campaign.getBudget());
        summary.setSpent(campaign.getSpent());
        summary.setRemaining(campaign.getBudget() - campaign.getSpent());
        summary.setPercentageUsed(
            campaign.getBudget() > 0
                ? Math.round((campaign.getSpent() / campaign.getBudget()) * 10000.0) / 100.0
                : 0.0
        );

        return summary;
    }

    public List<Expense> getExpensesByCampaign(Long campaignId) {
        getCampaignById(campaignId);
        return repository.findExpensesByCampaignId(campaignId);
    }

    public Expense addExpense(Long campaignId, Expense expense) {
        Campaign campaign = getCampaignById(campaignId);
        Map<String, Object> before = AuditLog.campaignToMap(campaign);
        expense.setCampaignId(campaignId);
        campaign.setSpent(campaign.getSpent() + expense.getAmount());
        Expense saved = repository.saveExpense(expense);
        Map<String, Object> after = AuditLog.campaignToMap(campaign);
        auditLogService.record("campaign", campaignId, "expense_added",
                resolveCurrentUser(), before, after);
        return saved;
    }

    public GlobalBudgetSummary getGlobalBudgetSummary() {
        List<Campaign> active = repository.findAll().stream()
                .filter(c -> "active".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());

        double totalBudget    = active.stream().mapToDouble(Campaign::getBudget).sum();
        double totalSpent     = active.stream().mapToDouble(Campaign::getSpent).sum();
        double totalAvailable = totalBudget - totalSpent;
        double pct = totalBudget > 0
                ? Math.round((totalSpent / totalBudget) * 10000.0) / 100.0
                : 0.0;

        GlobalBudgetSummary summary = new GlobalBudgetSummary();
        summary.setActiveCampaigns(active.size());
        summary.setTotalBudget(totalBudget);
        summary.setTotalSpent(totalSpent);
        summary.setTotalAvailable(totalAvailable);
        summary.setConsumptionPercentage(pct);
        return summary;
    }

    public Campaign updateCampaign(Long id, Campaign patch) {
        if (patch.getBudget() != null && patch.getBudget() < 0) {
            throw new IllegalArgumentException("Budget must be >= 0");
        }
        Campaign updated = repository.updateCampaign(id, patch);
        log.info("[TC-204] PUT /campaigns/{} → Campaña actualizada correctamente | name='{}' client='{}' budget={} status={}",
                updated.getId(), updated.getName(), updated.getClient(), updated.getBudget(), updated.getStatus());
        return updated;
    }

    public Campaign updateStatus(Long id, String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        Campaign patch = new Campaign();
        patch.setStatus(status);
        Campaign updated = repository.updateCampaign(id, patch);
        log.info("[TC-201] PATCH /campaigns/{}/status → Estado actualizado correctamente | name='{}' estado anterior → nuevo estado='{}'",
                updated.getId(), updated.getName(), updated.getStatus());
        return updated;
    }

    public Campaign updateBudget(Long campaignId, Double newBudget) {
        Campaign campaign = getCampaignById(campaignId);
        campaign.setSpent(0.0);
        campaign.setBudget(newBudget);
        return campaign;
    }

    private String resolveCurrentUser() {
        String user = AuditInterceptor.CurrentUser.get();
        return user != null ? user : "unknown";
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }
}
