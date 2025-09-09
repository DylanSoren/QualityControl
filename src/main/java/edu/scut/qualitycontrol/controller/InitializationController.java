package edu.scut.qualitycontrol.controller;

import edu.scut.qualitycontrol.service.DatabaseInitializationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class InitializationController {

    private final DatabaseInitializationService initializationService;

    // Spring 自动注入 DatabaseInitializationService
    public InitializationController(DatabaseInitializationService initializationService) {
        this.initializationService = initializationService;
    }

    /**
     * 定义一个 POST 端点，用于触发数据库初始化。
     */
    @PostMapping("/init-database")
    public ResponseEntity<String> triggerDatabaseInitialization() {
        try {
            // 在这里调用 Service 的核心方法
            initializationService.initializeDatabase();
            return ResponseEntity.ok("数据库初始化成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("数据库初始化失败: " + e.getMessage());
        }
    }
}