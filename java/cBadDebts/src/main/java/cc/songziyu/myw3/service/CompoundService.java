package cc.songziyu.myw3.service;

import cc.songziyu.myw3.bean.TokenConfigBean;
import ch.qos.logback.core.subst.Token;
import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.abi.DefaultFunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.utils.Numeric;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
public class CompoundService {
    private static final Log log = LogFactory.getLog(CompoundService.class);
    private static final String COMPTROLLER_ADDR = "0x3d9819210A31b4961b30EF54bE2aeD79B9c9Cd3B";
    private static final String UNISWAP_ANCHORED_ADDR = "0x50ce56a3239671ab62f185704caedf626352741e";
    private static final String CUSDC_ADDR = "0x39AA39c021dfbaE8faC545936693aC917d5E7563";
    private static final String CUNI_ADDR = "0x35A18000230DA775CAc24873d00Ff85BccdeD550";
    private static final int LOG_STEP_SIZE = 3000;
    private static final String FILE_ALL_TOKEN = "compoundv2_token_config.csv";
    @Autowired
    private IEvmService evmService;

    public String getPriceOracle() {
        Function function = new Function("oracle",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Address>() {
                }));
        try {
            List<Type> rets = evmService.ethCall(null, COMPTROLLER_ADDR, function);
            if (rets == null || rets.isEmpty()) {
                log.error("fail to get price oracle");
                return null;
            }
            return rets.get(0).getValue().toString();
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private List<EthLog.LogResult> getEvent(int minBlockNum, int maxBlockNum, String contractAddr, List<String[]> optionalTopics, List<String> singleTopics) {
        List<EthLog.LogResult> events = new ArrayList<>();
        int startBlockNum = minBlockNum;
        int endBlockNum = startBlockNum + LOG_STEP_SIZE - 1;
        while (startBlockNum < maxBlockNum) {
            if (endBlockNum > maxBlockNum)  endBlockNum = maxBlockNum;
            log.info("startBlock is " + startBlockNum + ", endBlock is " + endBlockNum);
            EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.valueOf(startBlockNum)),
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(endBlockNum)), contractAddr);
            try {
                if (optionalTopics != null)
                    for (String[] optionalTopic : optionalTopics)
                        filter.addOptionalTopics(optionalTopic);
                if (singleTopics != null)
                    for (String singleTopic : singleTopics)
                        filter.addSingleTopic(singleTopic);
                events.addAll(evmService.getLogs(filter));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return null;
            }
            startBlockNum = endBlockNum + 1;
            endBlockNum = endBlockNum + LOG_STEP_SIZE;
        }
        return events;
    }

    public List<EthLog.LogResult> getNewPriceOracleEvent() {
        List<EthLog.LogResult> events = new ArrayList<>();
        // 2024-02-23T14:12:35+00:00
        int minBlockNum = 19290806;
        // 2024-03-15T06:24:11+00:00
        int maxBlockNum = 19438542;
        int startBlockNum = minBlockNum;
        int endBlockNum = startBlockNum + LOG_STEP_SIZE - 1;
        String topic = Hash.sha3String("NewPriceOracle(address,address)");
        while (startBlockNum < maxBlockNum) {
            log.info("startBlock is " + startBlockNum + ", endBlock is " + endBlockNum);
            EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.valueOf(startBlockNum)),
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(endBlockNum)), COMPTROLLER_ADDR);
            filter.addSingleTopic(topic);
            try {
                events.addAll(evmService.getLogs(filter));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return null;
            }
            startBlockNum = endBlockNum + 1;
            endBlockNum = endBlockNum + LOG_STEP_SIZE;
        }
        return events;
    }

    public void getCuniTokenInfo() {
        getCTokenInfo(CUNI_ADDR);
    }

    private void getCTokenInfo(String tokenAddr) {
        try {
            log.info("token name is " + evmService.erc20Name(tokenAddr));
            log.info("token symbol is " + evmService.erc20Symbol(tokenAddr));
            Gson gson = new Gson();
            log.info(gson.toJson(getTokenConfigByCToken(tokenAddr)));
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public TokenConfigBean getTokenConfig(String functionName, List<Type> params) throws ExecutionException, InterruptedException {
        Function function = new Function(functionName, params,
                Arrays.asList(new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }, new TypeReference<Bytes32>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }));
        List<Type> rets = evmService.ethCall(null, UNISWAP_ANCHORED_ADDR, function);
        TokenConfigBean config = new TokenConfigBean();
        config.setcToken(rets.get(0).getValue().toString());
        config.setUnderlying(rets.get(1).getValue().toString());
        byte[] symbolHash = (byte[])rets.get(2).getValue();
        config.setSymbolHash(Numeric.toHexString((byte[])rets.get(2).getValue()));
        config.setBaseUint((BigInteger)rets.get(3).getValue());
        config.setPriceSource(((BigInteger)rets.get(4).getValue()).intValue());
        config.setFixedPrice((BigInteger) rets.get(5).getValue());
        config.setUniswapMarket(rets.get(6).getValue().toString());
        config.setReporter(rets.get(7).getValue().toString());
        config.setReporterMultiplier((BigInteger)rets.get(8).getValue());
        return config;

    }

    public TokenConfigBean getTokenConfigByCToken(String cToken) throws ExecutionException, InterruptedException {
        return getTokenConfig("getTokenConfigByCToken", Arrays.asList(new Address(cToken)));
    }

    public TokenConfigBean getTokenConfigByIndex(int i) throws ExecutionException, InterruptedException {
        return getTokenConfig("getTokenConfig", Arrays.asList(new Uint256(i)));
    }

    public void getAllTokenConfig() {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(FILE_ALL_TOKEN));
            String titles = "cTokenName,cTokenSymbol,cToken,underlying,symbolHash,baseUnit,priceSource,fixedPrice,uniswapMarket,reporter,reporterMultiplier,isUniswapReversed\n";
            bw.write(titles);
            for (int i = 0; i < 21; i++) {
                log.info("fetch token index " + i);
                TokenConfigBean config = getTokenConfigByIndex(i);
                String cToken = config.getcToken();
                String underlying = config.getUnderlying();
                String symbolHash = config.getSymbolHash();
                BigInteger baseUint = config.getBaseUint();
                Integer priceSource = config.getPriceSource();
                BigInteger fixedPrice = config.getFixedPrice();
                String uniswapMarket = config.getUniswapMarket();
                String reporter = config.getReporter();
                BigInteger reporterMultiplier = config.getReporterMultiplier();
                String cTokenName = evmService.erc20Name(cToken);
                String cTokenSymbol = evmService.erc20Symbol(cToken);
                String record = cTokenName + "," + cTokenSymbol + "," + cToken + "," + underlying + "," + symbolHash + "," + baseUint + ","
                        + priceSource + "," + fixedPrice + "," + uniswapMarket + "," + reporter + "," + reporterMultiplier + "\n";
                bw.write(record);
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    public List<EthLog.LogResult> getUniswapAnchoredViewEvent(int fromBlockNum, int toBlockNum) {
        return getEvent(fromBlockNum, toBlockNum, UNISWAP_ANCHORED_ADDR, null, null);
    }

    public void decodeUniswapAnchoredViewEvent(List<EthLog.LogResult> events, String fn) {
        /*
        ///@notice The event emitted when new prices are posted but the stored price is not updated due to the anchor
        event PriceGuarded( bytes32 indexed symbolHash, uint256 reporterPrice, uint256 anchorPrice );

        /// @notice The event emitted when the stored price is updated
        event PriceUpdated(bytes32 indexed symbolHash, uint256 price);

        /// @notice The event emitted when failover is activated
        event FailoverActivated(bytes32 indexed symbolHash);

        /// @notice The event emitted when failover is deactivated
        event FailoverDeactivated(bytes32 indexed symbolHash);
        */
        String priceGuardedHash = Hash.sha3String("PriceGuarded(bytes32,uint256,uint256)");
        String priceUpdatedHash = Hash.sha3String("PriceUpdated(bytes32,uint256)");
        String failOverActivatedHash = Hash.sha3String("FailoverActivated(bytes32)");
        String failoverDeactivated = Hash.sha3String("FailoverDeactivated(bytes32)");

        Map<String, String> symbolHashMap = new HashMap<>();
        symbolHashMap.put("0xfba01d52a7cd84480d0573725899486a0b5e55c20ff45d6628874349375d1650", "cUNI");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fn))) {
            String titles = "blockNum,blockTime,transactionHash,transactionIndex,address,data,topic1,topic2,topic3,topic4,topic5\n";
            bw.write(titles);
            for (EthLog.LogResult<org.web3j.protocol.core.methods.response.Log> event : events) {
                org.web3j.protocol.core.methods.response.Log ethLog = event.get();
                BigInteger blockNum = ethLog.getBlockNumber();
                String blockTime = "";
                EthBlock.Block block = evmService.getBlockbyNumber(blockNum, false);
                if (block != null) {
                    blockTime = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(Instant.ofEpochSecond(block.getTimestamp().longValue()));
                }
                String transactionHash = ethLog.getTransactionHash();
                BigInteger transactionIndex = ethLog.getTransactionIndex();
                String address = ethLog.getAddress();
                String rawData = ethLog.getData();
                String decodedData = "";
                List<String> topics = ethLog.getTopics();
                List<String> decodedTopics = new ArrayList<>();
                String functionNameHash = topics.get(0);
                List<TypeReference<Type>> dataTypes = null;
                if (priceGuardedHash.equals(functionNameHash)) {
                    decodedTopics.add("priceGuarded");
                    dataTypes = Utils.convert(Arrays.asList(new TypeReference<Uint256>() {
                    }, new TypeReference<Uint256>() {
                    }));
                    for (Type val : DefaultFunctionReturnDecoder.decode(rawData, dataTypes)) {
                        decodedData = decodedData + ((BigInteger) val.getValue()).toString() + " ";
                    }
                } else if (priceUpdatedHash.equals(functionNameHash)) {
                    decodedTopics.add("priceUpdate");
                    dataTypes = Utils.convert(Arrays.asList(new TypeReference<Uint256>() {
                    }));
                    for (Type val : DefaultFunctionReturnDecoder.decode(rawData, dataTypes)) {
                        decodedData = decodedData + ((BigInteger) val.getValue()).toString() + " ";
                    }
                } else if (failOverActivatedHash.equals(functionNameHash)) {
                    decodedTopics.add("failOverActivated");
                } else if (failoverDeactivated.equals(functionNameHash)) {
                    decodedTopics.add("failOverDeactivated");
                }
                if (symbolHashMap.containsKey(topics.get(1))) {
                    decodedTopics.add(symbolHashMap.get(topics.get(1)));
                } else {
                    decodedTopics.add(topics.get(1));
                }

                StringBuilder sb = new StringBuilder();
                String sep = ",";
                sb.append(blockNum).append(sep)
                        .append(blockTime).append(sep)
                        .append(transactionHash).append(sep)
                        .append(transactionIndex).append(sep)
                        .append(address).append(sep)
                        .append(decodedData).append(sep);
                for (String topic : decodedTopics) {
                    sb.append(topic).append(sep);
                }
                sb.append("\n");
                bw.write(sb.toString());
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void getUSDCCollateralFactor() {
        getCollateralFactor(CUSDC_ADDR);
    }

    private void getCollateralFactor(String tokenAddr) {
        Function function = new Function("markets", Arrays.asList(new Address(tokenAddr)),
                Arrays.asList(new TypeReference<Bool>() {},
                        new TypeReference<Uint>(){},
                        new TypeReference<Bool>(){}
                ));
        try {
            List<Type> rets = evmService.ethCall(null, COMPTROLLER_ADDR, function);
            if (rets == null || rets.isEmpty()) {
                log.error("fail to get collateral factor.");
            }
            Boolean isListed = (Boolean)rets.get(0).getValue();
            BigInteger collateralFactorMantissa = (BigInteger)rets.get(1).getValue();
            Boolean isComped = (Boolean)rets.get(2).getValue();
            log.info("CToken:" + tokenAddr + "\nisListed:" + isListed
            + "\ncollateralFactorMantissa:" + collateralFactorMantissa.toString()
            + "\nisComped:" + isComped);
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public Integer getAnchorPeriod() {
        BigInteger anchordPeriod = evmService.getUint32Var(UNISWAP_ANCHORED_ADDR, "anchorPeriod");
        if (anchordPeriod != null)
            return anchordPeriod.intValue();
        return null;
    }

    public Long getUpperBoundAnchorRatio() {
        BigInteger upperBoundAnchorRation = evmService.getUint256Var(UNISWAP_ANCHORED_ADDR, "upperBoundAnchorRatio");
        if (upperBoundAnchorRation == null) return null;
        return upperBoundAnchorRation.longValue();
    }

    public Long getLowerBoundAnchorRatio() {
        BigInteger lowerBoundAnchorRatio = evmService.getUint256Var(UNISWAP_ANCHORED_ADDR, "lowerBoundAnchorRatio");
        if (lowerBoundAnchorRatio == null)  return null;
        return lowerBoundAnchorRatio.longValue();
    }

    public BigInteger getAccountLiquidity(String account) {
        Function function = new Function("getAccountLiquidity", Arrays.asList(new Address(account)),
                Arrays.asList(new TypeReference<Uint>() {  // error
                }, new TypeReference<Uint>() {  // liquidity
                }, new TypeReference<Uint>() {  // shortfall
                }));
        try {
            List<Type> rets = evmService.ethCall(null, COMPTROLLER_ADDR, function);
            BigInteger error = (BigInteger)rets.get(0).getValue();
            if (error.intValue() != 0) {
                log.error("getAccountLiquidity return error, error code is " + error);
                return null;
            } else {
                BigInteger liquidity = (BigInteger) rets.get(1).getValue();
                BigInteger shortfall = (BigInteger) rets.get(2).getValue();
                if (liquidity.longValue() > 0) {
                    log.info("liqudity is grater than 0.");
                    return liquidity;
                } else {
                    log.info("shortfall");
                    return shortfall;
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public BigInteger getBorrowBalanceCurrent(String account, String cToken) {
        Function function = new Function("borrowBalanceCurrent", Arrays.asList(new Address(account)),
                Arrays.asList(new TypeReference<Uint>() {
                }));
        try {
            List<Type> rets = evmService.ethCall(null, cToken, function);
            BigInteger borrow = ((Uint)rets.get(0)).getValue();
            return borrow;
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public BigInteger getExchangeRate(String cToken) {
        return evmService.getUintVar(cToken, "exchangeRateCurrent");
    }

    public BigInteger getSuppliedBalanceCurrent(String account, String cToken) {
        BigInteger cBalance = evmService.erc20BalanceOf(cToken, account);
        if (cBalance.signum() == 0)
            return BigInteger.ZERO;
        /* exchangeRate between cToken and underlying token
         * exchangeRate = (getCash() + totalBorrows() - totalReserves()) / totalSupply()
         *
         * RETURN: The current exchange rate as an unsigned integer, scaled by 1 * 10^(18 - 8 + Underlying Token Decimals).
         *
         * see More: https://docs.compound.finance/v2/ctokens/#exchange-rate
         */
        BigInteger exchangeRate = getExchangeRate(cToken);
        return cBalance.multiply(exchangeRate);
    }

    private List<TokenConfigBean> getAllTokenConfigFromFile() {
        List<TokenConfigBean> tokenConfigs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_ALL_TOKEN));) {
            /* skip the first titles line */
            String line = br.readLine();
            while ( (line = br.readLine()) != null) {
                String[] fields = line.split(",");
                String symbol = fields[1];
                String cToken = fields[2];
                String underlying = fields[3];
                TokenConfigBean tokenConfig = new TokenConfigBean();
                tokenConfig.setSymbol(fields[1]);
                tokenConfig.setcToken(fields[2]);
                tokenConfig.setUnderlying(fields[3]);
                // TODO set other fields
                tokenConfigs.add(tokenConfig);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return tokenConfigs;
    }

    public void printAllBorrowedAndSupplied(String account) {
        for (TokenConfigBean config : getAllTokenConfigFromFile()) {
            printBorrowedAndSupplied(account, config);
        }
    }

    public void printBorrowedAndSupplied(String account, TokenConfigBean tokenConfig) {
        String symbol = tokenConfig.getSymbol();
        String cToken = tokenConfig.getcToken();
        String underlying = tokenConfig.getUnderlying();
        BigInteger borrowed = getBorrowBalanceCurrent(account, cToken);
        BigInteger supplied = getSuppliedBalanceCurrent(account, cToken);

        Integer cDecimal = evmService.erc20Decimals(cToken);
        Integer uDecimal = 18;
        // cEth's underlying is 0x0000000000000000000000000000000000000000
        if (!underlying.equals("0x0000000000000000000000000000000000000000")) {
            uDecimal = evmService.erc20Decimals(underlying);

        }
        BigDecimal borrowedDecimal = new BigDecimal(borrowed);
        BigDecimal suppliedDecimal = new BigDecimal(supplied);
        borrowedDecimal = borrowedDecimal.multiply(BigDecimal.valueOf(1, uDecimal));
        suppliedDecimal = suppliedDecimal.multiply(BigDecimal.valueOf(1, 10+uDecimal+cDecimal));
        log.info(symbol + " status for " + account + " borrowed " + borrowedDecimal.setScale(4, RoundingMode.HALF_EVEN)
                + " supplied: " + suppliedDecimal.setScale(4, RoundingMode.HALF_EVEN));
    }
}