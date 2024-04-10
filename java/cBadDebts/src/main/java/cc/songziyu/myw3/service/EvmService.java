package cc.songziyu.myw3.service;

import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.*;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component("evmService")
public class EvmService implements IEvmService {
    Log log = LogFactory.getLog(EvmService.class);

    @Value("${evm.rpcUrl}")
    private String rpcUrl = "https://mainnet.era.zksync.io";
    // eth: 1, zk: 324,  bnb: 56
    private long chainId;
    private Web3j w3;
    protected Web3jService w3Service;

    public EvmService() {
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public long getChainId() {
        return chainId;
    }

    public void setChainId(long chainId) {
        this.chainId = chainId;
    }

    @PostConstruct
    public void init() throws IOException {
        this.w3Service = new HttpService(rpcUrl);
        w3 = Web3j.build(w3Service);
        this.chainId = getChainIdFromServer().longValue();
    }

    @Override
    public BigInteger getBlockNum() {
        try {
            EthBlockNumber bn = w3.ethBlockNumber().send();
            return bn.getBlockNumber();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getClientVersion() {
        try {
            Web3ClientVersion web3ClientVersion = w3.web3ClientVersion().send();
            return web3ClientVersion.getWeb3ClientVersion();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public BigInteger getChainIdFromServer() throws IOException {
        return w3.ethChainId().send().getChainId();
    }

    @Override
    public EthBlock.Block getBlockbyNumber(BigInteger blockNum, boolean fullTransaction) {
        try {
            return w3.ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNum), fullTransaction).send().getBlock();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public BigInteger estimateCreateContractGas(String from, String inputData) throws IOException {
        return estimateGas(Transaction.createContractTransaction(from, BigInteger.ZERO, BigInteger.ZERO, inputData));
    }

    public BigInteger estimateGas(String from, String to, String inputData) throws IOException {
        return estimateGas(from, to, BigInteger.ZERO, inputData);
    }

    public BigInteger estimateGas(String from, String to, BigInteger value, String inputData) throws IOException {
        Transaction transaction = Transaction.createFunctionCallTransaction(from, BigInteger.ZERO,
                BigInteger.ZERO, new BigInteger("0"), to, value, inputData);
        return estimateGas(transaction);
    }

    public BigInteger estimateGas(Transaction transaction) throws IOException {
        EthEstimateGas resp =  w3.ethEstimateGas(transaction).send();
        if (resp.hasError()) {
            log.error("fail to estimate gas, msg: " + resp.getError().getMessage()
            + "  code:" + resp.getError().getCode());
            return null;
        } else {
            return resp.getAmountUsed();
        }
    }

    @Override
    public BigInteger getGasPrice() throws IOException {
        EthGasPrice resp = w3.ethGasPrice().send();
        return resp.getGasPrice();
    }

    public BigInteger getTransactionCount(String from) {
        try {
            EthGetTransactionCount resp = w3.ethGetTransactionCount(from, DefaultBlockParameterName.LATEST).send();
            return resp.getTransactionCount();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BigInteger getBalance(String from) throws IOException {
        return w3.ethGetBalance(from, DefaultBlockParameterName.LATEST).send().getBalance();
    }

    @Override
    public String deploy(Credentials credentials, String contractBinary) throws CipherException, IOException, TransactionException {
        String from = credentials.getAddress();
        BigInteger gasPrice = getGasPrice();
        BigInteger nonce = getTransactionCount(from);
        BigInteger gasLimit = estimateCreateContractGas(from, contractBinary);
        if (gasLimit == null) {
            return null;
        }
        RawTransaction tran = RawTransaction.createContractTransaction(nonce,
                gasPrice, gasLimit, new BigInteger("0"),
                contractBinary);
        byte[] signedMsg = TransactionEncoder.signMessage(tran, chainId, credentials);
        String signedMsgHex = Numeric.toHexString(signedMsg);
        EthSendTransaction resp = w3.ethSendRawTransaction(signedMsgHex).send();
        if (resp.hasError()) {
            System.out.println("fail to deploy: " + resp.getError().getMessage());
        } else {
            String tranHash = resp.getResult();
            EthGetTransactionReceipt receiptResp = w3.ethGetTransactionReceipt(tranHash).send();
            if (receiptResp.hasError()) {
                System.out.println("fail to get receipt: " + receiptResp.getError().getMessage());
            } else if (receiptResp.getTransactionReceipt().isEmpty()) {
                System.out.println("receipt is empty");
            } else {
                TransactionReceipt receipt = receiptResp.getTransactionReceipt().get();
                return receipt.getContractAddress();
            }
        }
        return null;
    }

    /*
     * interact with storage contract
     * @Return transaction Hash or null if it failed
     * */
    public String storage(Credentials credentials, String contractAddr) throws ExecutionException, InterruptedException, IOException {
        String from = credentials.getAddress();
        Function function = new Function("store",
                Arrays.asList(new Uint256(2024)),
                Arrays.asList());
        String encodedFunction = FunctionEncoder.encode(function);
        BigInteger gasPrice = getGasPrice();
        BigInteger nonce = getTransactionCount(from);
        BigInteger gasLimit = estimateGas(from, contractAddr, encodedFunction);
        RawTransaction transaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit,
                contractAddr, encodedFunction);

        byte[] signedMsg = TransactionEncoder.signMessage(transaction, chainId, credentials);
        String signedMsgHex = Numeric.toHexString(signedMsg);
        EthSendTransaction resp = w3.ethSendRawTransaction(signedMsgHex).send();
        if (resp.hasError()) {
            System.out.println("fail to deploy: " + resp.getError().getMessage());
        } else {
            String tranHash = resp.getResult();
            return tranHash;
//            EthGetTransactionReceipt receiptResp = w3.ethGetTransactionReceipt(tranHash).send();
//            if (receiptResp.hasError()) {
//                System.out.println("fail to get receipt: " + receiptResp.getError().getMessage());
//            } else if (receiptResp.getTransactionReceipt().isEmpty()) {
//                System.out.println("receipt is empty");
//            } else {
//                TransactionReceipt receipt = receiptResp.getTransactionReceipt().get();
//                return receipt.getTransactionHash();
//            }
        }
        return null;
    }


    public void retrieve(String from, String contractAddr) throws ExecutionException, InterruptedException, IOException {
        Function function = new Function("retrieve",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = w3.ethCall(
                        Transaction.createEthCallTransaction(from,
                                contractAddr, encodedFunction),
                        DefaultBlockParameterName.LATEST)
                .send();

        if (response.hasError()) {
            log.info("response has error:" + response.getError().getMessage());
        } else if (response.isReverted()) {
            log.info("reverted: " + response.getRevertReason());
        } else {
            List<Type> respValues = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
            for (Type respValue : respValues) {
                log.info(respValue.getValue());
            }
            if (respValues.isEmpty()) {
                log.info("empty return.");
            }
        }
    }

    /**
     * 获取Uniswap V3 USDC-ETH pool的状态，可以得到ETH以USDC计价的价格信息
     */
    public  void  getUniswapState() throws ExecutionException, InterruptedException {
        Function function = new Function(
                "slot0",
                Arrays.asList(),  // Solidity Types in smart contract functions
                Arrays.asList(new TypeReference<Uint160>() {
                              },
                        new TypeReference<Int24>() {
                        },
                        new TypeReference<Uint16>() {
                        },
                        new TypeReference<Uint16>() {
                        },
                        new TypeReference<Uint16>() {
                        },
                        new TypeReference<Uint8>() {
                        },
                        new TypeReference<Bool>() {
                        }
                ));

        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = w3.ethCall(
                        Transaction.createEthCallTransaction("0x8c29d85955699C2F649Cd59545E8a22946Ef217D",
                                "0x88e6A0c2dDD26FEEb64F039a2c41296FcB3f5640", encodedFunction),
                        DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        List<Type> respValues = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        for (Type respValue : respValues) {
            log.info(respValue.getValue());
        }
    }

    @Override
    public String sendRawTransaction(Credentials cred, String to, BigInteger value, String encodedFunction) throws IOException {
        String from = cred.getAddress();
        BigInteger gasPrice = getGasPrice();
        BigInteger nonce = getTransactionCount(from);
        BigInteger gasLimit = estimateGas(from, to, value, encodedFunction);
        if (gasLimit == null) {
            return null;
        }
        RawTransaction transaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit,
                to, value, encodedFunction);

        byte[] signedMsg = TransactionEncoder.signMessage(transaction, getChainId(), cred);
        String signedMsgHex = Numeric.toHexString(signedMsg);
        EthSendTransaction resp = w3.ethSendRawTransaction(signedMsgHex).send();
        if (resp.hasError()) {
            log.error("fail to sendRawTransaction: " + resp.getError().getMessage());
        } else {
            return resp.getResult();
        }
        return null;
    }

    @Override
    public String sendRawTransaction(Credentials cred, String to, BigInteger value, Function function) throws IOException {
        String from = cred.getAddress();
        String encodedFunction = FunctionEncoder.encode(function);
        BigInteger gasPrice = getGasPrice();
        BigInteger nonce = getTransactionCount(from);
        BigInteger gasLimit = estimateGas(from, to, value, encodedFunction);
        if (gasLimit == null) {
            return null;
        }
        RawTransaction transaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit,
                to, value, encodedFunction);

        byte[] signedMsg = TransactionEncoder.signMessage(transaction, getChainId(), cred);
        String signedMsgHex = Numeric.toHexString(signedMsg);
        EthSendTransaction resp = w3.ethSendRawTransaction(signedMsgHex).send();
        if (resp.hasError()) {
            log.error("fail to sendRawTransaction: " + resp.getError().getMessage());
        } else {
            return resp.getResult();
        }
        return null;
    }

    @Override
    public String sendRawTransaction(Credentials cred, String to, Function function) throws IOException {
        return sendRawTransaction(cred, to, BigInteger.ZERO, function);
    }

    public TransactionReceipt getTransactionReceipt(String tranHash) throws IOException {
        EthGetTransactionReceipt resp = w3.ethGetTransactionReceipt(tranHash).send();
        if (resp.hasError()) {
            log.info("fail to get receipt for " + tranHash + " :" + resp.getError().getMessage());
            return null;
        } else {
            return resp.getTransactionReceipt().get();
        }
    }

    @Override
    public  List<Type> ethCall(String from, String to, Function function) throws ExecutionException, InterruptedException {
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = w3.ethCall(
                        Transaction.createEthCallTransaction(from,
                                to, encodedFunction),
                        DefaultBlockParameterName.LATEST)
                .sendAsync().get();
        if (response.hasError()) {
            log.error(response.getError().getMessage());
            return null;
        } else {
            List<Type> respValues = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
            return respValues;
        }
    }

    @Override
    public Address getAddrVar(String contractAddr, String functionName) throws ExecutionException, InterruptedException {
        Function function = new Function(functionName, Arrays.asList(),
                Arrays.asList(new TypeReference<Address>() {
                }));
        List<Type> rets = ethCall(null, contractAddr, function);
        if (rets == null || rets.isEmpty())
            return null;
        return (Address)rets.get(0).getValue();
    }

    @Override
    public  Bytes32 getBytes32Var(String contractAddr, String functionName) throws ExecutionException, InterruptedException {
        Function function = new Function(functionName, Arrays.asList(),
                Arrays.asList(new TypeReference<Bytes32>() {
                }));
        List<Type> rets = ethCall(null, contractAddr, function);
        if (rets == null || rets.isEmpty())  return null;
        return (Bytes32)rets.get(0).getValue();
    }

    @Override
    public BigInteger getUint256Var(String contractAddr, String functionName) {
        Function function = new Function(functionName, Arrays.asList(),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        try {
            List<Type> rets = ethCall(null, contractAddr, function);
            if (rets == null || rets.isEmpty()) return null;
            return ((Uint256)rets.get(0)).getValue();
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public BigInteger getUintVar(String contractAddr, String functionName) {
        Function function = new Function(functionName, Arrays.asList(),
                Arrays.asList(new TypeReference<Uint>() {
                }));
        try {
            List<Type> rets = ethCall(null, contractAddr, function);
            if (rets == null || rets.isEmpty()) return null;
            return ((Uint)rets.get(0)).getValue();
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;

    }


    public BigInteger getUint32Var(String contractAddr, String functionName) {
        Function function = new Function(functionName, Arrays.asList(),
                Arrays.asList(new TypeReference<Uint32>() {
                }));
        try {
            List<Type> rets = ethCall(null, contractAddr, function);
            return ((Uint32) rets.get(0)).getValue();
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getUtf8StringVar(String contractAddr, String functionName) throws ExecutionException, InterruptedException {
        Function function = new Function(functionName, Arrays.asList(),
                Arrays.asList(new TypeReference<Utf8String>() {
                }));
        List<Type> rets = ethCall(null, contractAddr, function);
        if (rets == null || rets.isEmpty())
            return null;
        return (String)rets.get(0).getValue();
    }

    @Override
    public  String erc20Name(String tokenAddr) {
        try {
            return getUtf8StringVar(tokenAddr, "name");
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String erc20Symbol(String tokenAddr) {
        try {
            return getUtf8StringVar(tokenAddr, "symbol");
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Integer erc20Decimals(String contractAddr) {
        Function function = new Function("decimals", Arrays.asList(),
                Arrays.asList(new TypeReference<Uint8>() {
                }));
        try {
            List<Type> rets = ethCall(null, contractAddr, function);
            return ((BigInteger)rets.get(0).getValue()).intValue();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public BigInteger erc20BalanceOf(String contractAddr, String userAddr) {
        Function function = new Function("balanceOf", Arrays.asList(new Address(userAddr)),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        try {
            List<Type> rets = ethCall(userAddr, contractAddr, function);
            return (BigInteger)rets.get(0).getValue();
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void getLogs(String contractAddr) throws IOException {
        EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(new BigInteger("7710671")),
                DefaultBlockParameter.valueOf(new BigInteger("7710971")), contractAddr);
        filter.addSingleTopic("0xd52b2b9b7e9ee655fcb95d2e5b9e0c9f69e7ef2b8e9d2d0ea78402d576d22e22");
        EthLog resp = w3.ethGetLogs(filter).send();
        if (resp.hasError()) {
            log.error(resp.getError().getMessage());
            return;
        }
        log.info("ethLog size:" + resp.getLogs().size());
        for (EthLog.LogResult ethLog : resp.getLogs()) {
            log.info(ethLog.toString());
        }
    }

    @Override
    public List<EthLog.LogResult> getLogs(EthFilter filter) throws IOException {
        EthLog resp = w3.ethGetLogs(filter).send();
        if (resp.hasError()) {
            log.error(resp.getError().getMessage());
            return null;
        }
        return resp.getLogs();
    }
}