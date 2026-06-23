package com.genius.budgetmanager.service;

import com.genius.budgetmanager.model.BudgetSummary;
import com.genius.budgetmanager.model.Campaign;
import com.genius.budgetmanager.model.Expense;
import com.genius.budgetmanager.model.GlobalBudgetSummary;
import com.genius.budgetmanager.repository.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    @Autowired
    private CampaignRepository repository;

    // Fix BM-F01/BM-F02: soportar filtros por status y cliente en el listado.
    public List<Campaign> getCampaigns(String status, String client) {
        final String normalizedStatus = normalize(status);
        final String normalizedClient = normalize(client);

        return repository.findAll().stream()
            .filter(c -> normalizedStatus == null || normalizedStatus.equals(normalize(c.getStatus())))
            .filter(c -> normalizedClient == null || normalizedClient.equals(normalize(c.getClient())))
                .collect(Collectors.toList());
    }

    // Fix BM-F01: habilitar alta de nuevas campanas desde API.
    public Campaign createCampaign(Campaign campaign) {
        if (campaign == null) {
            throw new RuntimeException("Campaign payload is required");
        }
        if (campaign.getName() == null || campaign.getName().isBlank()) {
            throw new RuntimeException("Campaign name is required");
        }
        if (campaign.getClient() == null || campaign.getClient().isBlank()) {
            throw new RuntimeException("Campaign client is required");
        }
        if (campaign.getBudget() == null || campaign.getBudget() < 0) {
            throw new RuntimeException("Campaign budget must be >= 0");
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

        return repository.saveCampaign(campaign);
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
        expense.setCampaignId(campaignId);
        campaign.setSpent(campaign.getSpent() + expense.getAmount());
        return repository.saveExpense(expense);
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

    public Campaign updateBudget(Long campaignId, Double newBudget) {
        Campaign campaign = getCampaignById(campaignId);
        campaign.setSpent(0.0);
        campaign.setBudget(newBudget);
        return campaign;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
