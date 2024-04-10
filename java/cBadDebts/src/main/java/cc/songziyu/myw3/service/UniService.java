package cc.songziyu.myw3.service;

import cc.songziyu.myw3.bean.ObservationBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.*;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class UniService {
    private static final Log log = LogFactory.getLog(UniService.class);
    @Autowired
    private IEvmService evmService;
    private static final String USDC_UNI_CONTRACT_ADDR = "0xd0fc8ba7e267f2bc56044a7715a489d851dc6d78";
    private static final String UNI_ETH_POOL_ADDR = "0x1d42064Fc4Beb5F8aAF85F4617AE8b3b5B8Bd801";
    private static final String USDC_ETH_POOL_ADDr = "0x8ad599c3a0ff1de082011efddc58f1908eb6e6d8";

    public void getUniswapState() throws ExecutionException, InterruptedException {
        String contractAddr = "0xd0fc8ba7e267f2bc56044a7715a489d851dc6d78";
        Function function = new Function(
                "slot0",
                Arrays.asList(),  // Solidity Types in smart contract functions
                Arrays.asList(new TypeReference<Uint160>() {  // sqrtPrice96
                              },
                        new TypeReference<Int24>() {  // tick
                        },
                        new TypeReference<Uint16>() {  // observationIndex
                        },
                        new TypeReference<Uint16>() {  // observationCardinality
                        },
                        new TypeReference<Uint16>() {  // observationCardinalityNext
                        },
                        new TypeReference<Uint8>() {  // feeProtocol
                        },
                        new TypeReference<Bool>() {  // unlocked
                        }
                ));
        List<Type> rets = evmService.ethCall(null, contractAddr, function);
        if (rets == null)  return;
        log.info(((BigInteger)rets.get(2).getValue()).intValue());
    }

    private void getObservation(String contractAddr, String fn) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(fn);
            fw.write("index,blockTime,tickCumulative,secondsPerLiquidityCumulativeX128,initialized\n");
            for (int i = 0; i < 65536; i++) {
                ObservationBean bean = getObservation(contractAddr, i);
                StringBuilder sb = new StringBuilder();
                sb.append(i);
                if (bean != null) {
                    sb.append(",");
                    sb.append(bean.getBlockTimeStamp());
                    sb.append(",");
                    sb.append(bean.getTickCumulative());
                    sb.append(",");
                    sb.append(bean.getSecondsPerLiquidityCumulativeX128());
                    sb.append(",");
                    sb.append(bean.getInitialized());
                }
                fw.write(sb.toString());
                fw.write("\n");

                if (!bean.getInitialized())
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        log.info("finished.");
    }

    public void getUniUsdcObservation() {
        getObservation(USDC_UNI_CONTRACT_ADDR, "usdc_uni_oracle.csv");
    }

    public void getUniEthObservation() {
        getObservation(UNI_ETH_POOL_ADDR, "uni_eth_oracle.csv");
    }

    public void getUsdcEthObservation() {
        getObservation(USDC_ETH_POOL_ADDr, "usdc_eth_oracle.csv");
    }

    public ObservationBean getObservation(String contractAddr, int index) {
        Function function = new Function("observations",
                Arrays.asList(new Uint(BigInteger.valueOf(index))),
                Arrays.asList(new TypeReference<Uint32>() {  // blockTimestamp
                              },
                        new TypeReference<Int56>() {  // tickCumulative
                        },
                        new TypeReference<Uint160>() {  // secondsPerLiquidityCumulativeX128
                        },
                        new TypeReference<Bool>() {  // initialized
                        }));
        try {
            List<Type> rets = evmService.ethCall(null, contractAddr, function);
            if (rets == null)  return null;
            int blockTimeStamp = ((BigInteger)rets.get(0).getValue()).intValue();
            BigInteger tickCumulative = (BigInteger)rets.get(1).getValue();
            BigInteger secondsPerLiquidityCumulativeX128 = (BigInteger)rets.get(2).getValue();
            Boolean initialized = (Boolean)rets.get(3).getValue();
            log.info("blockTimeStam: " + blockTimeStamp
                    + "\ntickCumulative: " + tickCumulative
            + "\nsecondsPerLiquidityCumulative: " + secondsPerLiquidityCumulativeX128
            + "\ninitialized: " + initialized);
            return new ObservationBean(blockTimeStamp, tickCumulative, secondsPerLiquidityCumulativeX128, initialized);
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}