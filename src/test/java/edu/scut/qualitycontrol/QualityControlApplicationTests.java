package edu.scut.qualitycontrol;

import edu.scut.qualitycontrol.service.DatabaseInitializationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class QualityControlApplicationTests {
    @Autowired
    private DatabaseInitializationService initializationService;
    @Test
    void initTest() throws Exception {
        initializationService.initializeDatabase();
    }

}
