package cc.songziyu.myw3.service;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface IEvmService {

    BigInteger getChainIdFromServer() throws IOException;

    BigInteger getBlockNum();

    String getClientVersion();

    EthBlock.Block getBlockbyNumber(BigInteger blockNum, boolean fullTransaction);

    BigInteger getGasPrice() throws IOException;

    BigInteger estimateCreateContractGas(String from, String inputData) throws IOException;

    public String deploy(Credentials credentials, String contractBinary) throws CipherException, IOException, TransactionException;

    String sendRawTransaction(Credentials cred, String to, BigInteger value, String encodedFunction) throws IOException;

    String sendRawTransaction(Credentials cred, String to, BigInteger value, Function function) throws IOException;

    String sendRawTransaction(Credentials cred, String to, Function function) throws IOException;

    List<Type> ethCall(String from, String to, Function function) throws ExecutionException, InterruptedException;

    Address getAddrVar(String contractAddr, String functionName) throws ExecutionException, InterruptedException;

    Bytes32 getBytes32Var(String contractAddr, String functionName) throws ExecutionException, InterruptedException;
    BigInteger getUint256Var(String contractAddr, String functionName);

    BigInteger getUintVar(String contractAddr, String functionName);

    BigInteger getUint32Var(String contractAddr, String functionName);

    String getUtf8StringVar(String contractAddr, String functionName) throws ExecutionException, InterruptedException;

    /**
     * ERC20 related
     */
    String erc20Name(String tokenAddr);
    String erc20Symbol(String tokenAddr);
    Integer erc20Decimals(String contractAddr);
    BigInteger erc20BalanceOf(String contractAddr, String userAddr);

    void getLogs(String contractAddr) throws IOException;
    List<EthLog.LogResult> getLogs(EthFilter filter) throws IOException;

}