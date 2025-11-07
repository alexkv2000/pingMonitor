package kvo.monitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;

@Controller
public class PingController {

    @Autowired
    private PingService pingService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("servers", pingService.getPingData().keySet());
        return "index";
    }

    @GetMapping("/api/ping")
    @ResponseBody
    public Map<String, Object> getPingData() {
        return Map.of("data", pingService.getPingData());
    }
}