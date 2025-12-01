package kvo.monitor.ping;

import kvo.monitor.sizetable.SizeTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;

@Controller
public class PingController {
    @Autowired
    private PingService pingService;
    private SizeTableService sizeTableService;
    @Value("${server.admin}")
    private String serverAdmin;
    @GetMapping("/")
    public String index(Model model) throws UnknownHostException {
        model.addAttribute("servers", pingService.getPingData().keySet());
        model.addAttribute("serverAdmin",serverAdmin);
        String pcName = InetAddress.getLocalHost().getHostName();
        model.addAttribute("computerName", pcName);
        return "index";
    }
    @GetMapping("/column")
    public String indexCol(Model model) throws UnknownHostException {
        model.addAttribute("servers", pingService.getPingData().keySet());
        model.addAttribute("serverAdmin",serverAdmin);
        String pcName = InetAddress.getLocalHost().getHostName();
        model.addAttribute("computerName", pcName);
        return "indexCol";
    }
    @GetMapping("/help")
    public String help() {
        System.out.println("'/' - мониторинг пинга серверов; \n'/api/ping' - просмотр ответов в JSON формате; \n'/api/setPC?computerName=NameConputer' - добавить ПК для мониторинга; \n'/api/removePC?computerName=NameConputer' - удалить ПК из мониторинга; \n'/api/clearAllPingData' - очистить таблицу мониторинга.\n");
        return "help";
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
    @CrossOrigin(origins = "*")
    @GetMapping("/api/sizeTable/MSSQL")
    public ResponseEntity<List<Map<String, Object>>> getSizeTableMSSQL() {
        try {
            List<Map<String, Object>> data = SizeTableService.getSizeTableMSSQL();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    @GetMapping("/api/sizeTable")
    public String getSizeTable() {
        return "sizeTable";
    }
}