package cc.songziyu.myw3.task;

import cc.songziyu.myw3.service.IEvmService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ServerInfoTask {
    Log log = LogFactory.getLog(ServerInfoTask.class);
    @Autowired
    private IEvmService evmService;
    @Autowired
    private IEvmService zkSyncService;

    public void run() throws IOException {
        log.info("-------------------------------");
        log.info("Ethereum Network status:");
        serverInfo(evmService);
        log.info("-------------------------------");
        /*
        log.info("ZkSyncEra Network status:");
        serverInfo(zkSyncService);
        log.info("-------------------------------");
        */
    }

    public void serverInfo(IEvmService evmService) throws IOException {
        log.info("EVM RPC服务端版本: " + evmService.getClientVersion());
        log.info("当前区块高度: " + evmService.getBlockNum());
        log.info("当前gas价格(单位gwei): " + evmService.getGasPrice().doubleValue() / 1e9);
    }
}
