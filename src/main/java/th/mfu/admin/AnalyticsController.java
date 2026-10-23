package th.mfu.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import th.mfu.domain.Season;
import th.mfu.service.AnalyticsService;

/** Electricity / expense trends by month and by Thai season, drawn with Chart.js. */
@Controller
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/admin/analytics")
    public String analytics(Model model) {
        List<String> months = new ArrayList<>();
        List<Double> units = new ArrayList<>();
        List<Double> electricCost = new ArrayList<>();
        List<Double> totalBilled = new ArrayList<>();
        List<String> monthSeasons = new ArrayList<>();
        for (Map.Entry<String, double[]> e : analyticsService.monthlyTotals(12).entrySet()) {
            months.add(e.getKey());
            units.add(e.getValue()[0]);
            electricCost.add(e.getValue()[1]);
            totalBilled.add(e.getValue()[2]);
            monthSeasons.add(Season.of(Integer.parseInt(e.getKey().substring(5))).name());
        }
        model.addAttribute("months", months);
        model.addAttribute("units", units);
        model.addAttribute("electricCost", electricCost);
        model.addAttribute("totalBilled", totalBilled);
        model.addAttribute("monthSeasons", monthSeasons);

        Map<Season, Double> seasonAverages = analyticsService.seasonAverages();
        model.addAttribute("seasonAverages", seasonAverages);
        model.addAttribute("seasonLabels", seasonAverages.keySet().stream().map(Season::getLabel).toArray());
        model.addAttribute("seasonValues", seasonAverages.values());
        model.addAttribute("abnormal", analyticsService.abnormalRooms());
        return "admin/analytics";
    }
}
