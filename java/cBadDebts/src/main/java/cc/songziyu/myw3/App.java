package cc.songziyu.myw3;

import cc.songziyu.myw3.service.IEvmService;
import cc.songziyu.myw3.task.CompoundTask;
import cc.songziyu.myw3.task.ServerInfoTask;
import cc.songziyu.myw3.task.UniPriceOracleTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class App {
    public static String E9_STR = "1000000000";
    public static double E9_NUM = 1e9;
    Log log = LogFactory.getLog(App.class);

    @Autowired
    private ServerInfoTask serverInfoTask;
    @Autowired
    private IEvmService evmService;
    @Autowired
    private CompoundTask compoundBadDebtTask;
    @Autowired
    private UniPriceOracleTask uniPriceOracleTask;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public CommandLineRunner demo() {
        return args -> {
            try {
                // 获取服务端信息， 目的是检验代码基本配置是否正确
//                serverInfoTask.run();

                uniPriceOracleTask.run();
//                compoundBadDebtTask.run();

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        };
    }
}
