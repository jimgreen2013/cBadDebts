package cc.songziyu.myw3.task;

import cc.songziyu.myw3.service.CompoundService;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Component
public class CompoundTask {
    private org.apache.commons.logging.Log log = LogFactory.getLog(CompoundTask.class);
    @Autowired
    private CompoundService compoundService;

    public void run() {
        /* get All cToken configuration */
//        compoundService.getAllTokenConfig();

        /* print account financial state on Compound V2, namely supplied and borrowed assets */
//        printAccountLiquidity();

        /* get current price oracle address */
//        getPriceOracle();

//        getNewPriceOracleEvent();
//        compoundService.getCuniTokenInfo();
//        getUniswapAnchoredViewEvent();
//        printAnchordPeriod();
        printAnchorRation();
//        printUSDCCollateralFactor();
    }

    private void printAccountLiquidity() {
        List<String> accounts = Arrays.asList( "0x6980a47beE930a4584B09Ee79eBe46484FbDBDD0",
                "0x5968ada261a84e19a6c85830e655647752585ed4",
                "0x49bC3ceC1fb7978746f742a4E485d0D601831cEa",
                "0x2F99fb66Ea797E7fA2d07262402Ab38bd5e53B12",
                "0x146444424F6a61fc8Cc05dF9225A88217aBB032c");
//        List<String> accounts = Arrays.asList( "0x6980a47beE930a4584B09Ee79eBe46484FbDBDD0");
        for (String account : accounts) {
            log.info("------------------------------------------");
            BigInteger liquidity = compoundService.getAccountLiquidity(account);
            if (liquidity == null) {
                log.info("fail to get compound liquidity for account " + account);
            } else {
                log.info("account " + account + " liqudity on compound finance is " + liquidity);
            }
            compoundService.printAllBorrowedAndSupplied(account);
            log.info("------------------------------------------");
        }
    }

    private void getPriceOracle() {
        String priceOracleAddr = compoundService.getPriceOracle();
        System.out.println("price oracle addresss is:" + priceOracleAddr);
    }

    private void printEvent(List<EthLog.LogResult> events, String eventName) {
        System.out.println("will print " + eventName + " event:");
        if (events == null) {
            System.out.println("fail to get " +  eventName + " event.");
            return;
        } else if (events.isEmpty()) {
            System.out.println("No " + eventName + " event in the block range");
        } else {
            for (EthLog.LogResult<Log> event : events) {
                System.out.println(event.toString());
            }
        }
    }

    private void getNewPriceOracleEvent() {
        List<EthLog .LogResult> events = compoundService.getNewPriceOracleEvent();
        if (events == null) {
            System.out.println("fail to get NewPriceOracle event.");
            return;
        } else if (events.isEmpty()) {
            System.out.println("No NewPriceOracle event in the block range");
        } else {
            for (EthLog.LogResult<Log> event : events) {
                System.out.println(event.toString());
            }
        }
    }

    private void getCuniTokenInfo() {
        compoundService.getCuniTokenInfo();;
    }

    private void writeEthLogToFile(List<EthLog.LogResult> events, String fn) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fn));) {
            String titles = "blockNum,transactionHash,transactionIndex,address,data,topic1,topic2,topic3,topic4,topic5\n";
            bw.write(titles);
            for (EthLog.LogResult<Log> event : events) {
                Log ethLog = event.get();
                BigInteger blockNum = ethLog.getBlockNumber();
                String transactionHash = ethLog.getTransactionHash();
                BigInteger transactionIndex = ethLog.getTransactionIndex();
                String address = ethLog.getAddress();
                String data = ethLog.getData();
                List<String> topics = ethLog.getTopics();

                /* raw eth log */
                StringBuilder sb = new StringBuilder();
                sb.append(blockNum.toString()).append(",");
                sb.append(transactionHash).append(",");
                sb.append(transactionIndex.toString()).append(",");
                sb.append(address).append(",");
                sb.append(data).append(",");
                for (int i = 0; i < 5; i++) {
                    if (i < topics.size()) {
                        sb.append(topics.get(i));
                    }
                    sb.append(",");
                }
                sb.append("\n");
                bw.write(sb.toString());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void getUniswapAnchoredViewEvent() {
        // 2024-02-23T14:12:35+00:00
        int minBlockNum = 19290806;
        // 2024-02-23T14:52:35+00:00
        int maxBlockNum = 19291006;
        List<EthLog.LogResult> events = compoundService.getUniswapAnchoredViewEvent(minBlockNum, maxBlockNum);
        writeEthLogToFile(events, "com_uniswapAnchoredView_events.csv");
        compoundService.decodeUniswapAnchoredViewEvent(events, "com_uniswapAnchoredView_events_decoded.csv");
    }

    private void printAnchordPeriod() {
        Integer anchorPeriod = compoundService.getAnchorPeriod();
        if (anchorPeriod != null)
            System.out.println("anchordPeriod is " + anchorPeriod);
        else
            System.out.println("fail to get anchordPeriod");
    }

    private void printAnchorRation() {
        Long upperBound = compoundService.getUpperBoundAnchorRatio();
        if (upperBound != null)
            System.out.println("upperBound is " + upperBound);
        else
            System.out.println("fail to get upperBound.");
        Long lowerBound = compoundService.getLowerBoundAnchorRatio();
        if (lowerBound != null)
            System.out.println("lowerBound is " + lowerBound);
        else
            System.out.println("fail to get lowerBound.");

    }

    private void printUSDCCollateralFactor() {
        compoundService.getUSDCCollateralFactor();
    }
}
