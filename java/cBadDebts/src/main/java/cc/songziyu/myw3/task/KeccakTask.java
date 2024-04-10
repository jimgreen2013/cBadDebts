package cc.songziyu.myw3.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.web3j.abi.DefaultFunctionReturnDecoder;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class KeccakTask {
    private static Log log = LogFactory.getLog(KeccakTask.class);

    public static void main(String[] args) {
        log.info(Hash.sha3String("NewPriceOracle(address,address)"));
        log.info(Hash.sha3String("PriceGuarded(bytes32,uint256,uint256)"));
        log.info(Hash.sha3String("PriceUpdated(bytes32,uint256)"));
        log.info(Hash.sha3String("cUNI"));

        String raw = "0x0000000000000000000000000000000000000000000000000000000000837d6c00000000000000000000000000000000000000000000000000000000006faeaf";
        List<Type> rets = DefaultFunctionReturnDecoder.decode(raw, Utils.convert(Arrays.asList(new TypeReference<Uint256>() {
        }, new TypeReference<Uint256>() {
        })));
        for (Type ret : rets) {
            log.info(ret.getValue().toString());
        }
        log.info(TypeDecoder.decodeNumeric("0x00000000000000000000000000000000000000000000000000000000006f3211", Uint256.class).getValue().toString());
//        log.info(DateTimeFormatter.ISO_DATE_TIME.format(Instant.ofEpochSecond(10)));
        log.info(DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(Instant.ofEpochSecond(10)));
    }
}