package kvo.monitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    // добавление сервера для мониторинга Пингов
    @PostMapping ("/api/setPC")
    @ResponseBody
    public Map<String, Object> addComputer(@RequestParam String computerName) {
        pingService.addServer(computerName);
        return Map.of("status","success","massage","PC: " + computerName + " add to Monitoring");
    }
    // удаление выбранного сервера и его истории Пингов из мониторинга
    @PostMapping ("/api/removePC")
    @ResponseBody
    public Map <String, Object> removeComputer(@RequestParam String computerName) {
        pingService.removeServer(computerName);
        return Map.of("status","success","massage","PC: " + computerName + " deleted from Monitoring");
    }
    // Очистка всей истории Пингов по всем серверам.
    @PostMapping ("/api/clearAllPingData")
    @ResponseBody
    public Map <String, Object> clearAllPingData() {
        pingService.clearAllPingData();
        return Map.of("status","success","massage","All history clear");
    }
}