package cc.songziyu.myw3.task;

import cc.songziyu.myw3.bean.ObservationBean;
import cc.songziyu.myw3.service.UniService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class UniPriceOracleTask {
    private static final Log log = LogFactory.getLog(UniPriceOracleTask.class);
    @Autowired
    private UniService uniService;

    public void run() {
//        uniService.getUsdcEthObservation();
//        uniService.getUniEthObservation();
//        getPrice(1800, "uni_price_1800.csv");
        getPrice(900, "uni_price_900.csv");
    }

    private void getPrice(int minPeriod, String filename) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new FileReader("usdc_uni_oracle.csv"));
            String line = br.readLine();  // skip titles
            List<ObservationBean> beans = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                String strBlockTime = fields[1];
                String strTicketCumulative = fields[2];
                String strInitialized = fields[4];

                if (!"true".equals(strInitialized)) continue;

                ObservationBean bean = new ObservationBean();
                bean.setBlockTimeStamp(Integer.valueOf(strBlockTime));
                bean.setTickCumulative(new BigInteger(strTicketCumulative));
                beans.add(bean);
            }
            Collections.sort(beans, new Comparator<ObservationBean>() {
                @Override
                public int compare(ObservationBean o1, ObservationBean o2) {
                    return o1.getBlockTimeStamp() - o2.getBlockTimeStamp();
                }
            });

            bw = new BufferedWriter(new FileWriter(filename));
            bw.write("time,price,timeRange(seconds)\n");
            for (int i = 1; i < beans.size(); i++) {
                log.info(i + "/" + beans.size());
                ObservationBean currentObservation = beans.get(i);
                ObservationBean previousObservation = null;
                int deltaSeconds = 0;
                for (int j = i - 1; j >= 0; j--) {
                    previousObservation = beans.get(j);
                    deltaSeconds = currentObservation.getBlockTimeStamp() - previousObservation.getBlockTimeStamp();
                    if (deltaSeconds >= minPeriod)  break;
                }
                if (deltaSeconds < minPeriod)  continue;

                BigInteger deltaTickCumulative = currentObservation.getTickCumulative().subtract(previousObservation.getTickCumulative())
                        .divide(BigInteger.valueOf(deltaSeconds));

                BigDecimal base = new BigDecimal("1.0001");
                BigDecimal price = null;
                if (deltaTickCumulative.intValue() < 0) {
                    price = base.pow(-deltaTickCumulative.intValue());

                    price = BigDecimal.valueOf(1e12).divide(price, 2, RoundingMode.HALF_EVEN);
                } else {
                    price = base.pow(deltaTickCumulative.intValue()).multiply(BigDecimal.valueOf(1e12));
                }

                StringBuilder sb = new StringBuilder();
                String time = DateTimeFormatter.ISO_DATE_TIME
                        .withZone(ZoneId.of("UTC")) .format(Instant.ofEpochSecond(currentObservation.getBlockTimeStamp()));
                sb.append(time);
                sb.append(",");
                sb.append(price.toString());
                sb.append(",");
                sb.append(deltaSeconds);
                sb.append("\n");
                bw.write(sb.toString());
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
         }
    }
}
