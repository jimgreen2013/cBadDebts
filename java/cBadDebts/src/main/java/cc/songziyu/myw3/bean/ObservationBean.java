package cc.songziyu.myw3.bean;

import java.math.BigInteger;

public class ObservationBean {
    private int blockTimeStamp;
    private BigInteger tickCumulative;
    private BigInteger secondsPerLiquidityCumulativeX128;
    private Boolean initialized;

    public ObservationBean() {

    }

    public ObservationBean(int blockTimeStamp, BigInteger tickCumulative,
                           BigInteger secondsPerLiquidityCumulativeX128,
                           Boolean initialized) {
        this.blockTimeStamp = blockTimeStamp;
        this.tickCumulative = tickCumulative;
        this.secondsPerLiquidityCumulativeX128 = secondsPerLiquidityCumulativeX128;
        this.initialized = initialized;
    }

    public int getBlockTimeStamp() {
        return blockTimeStamp;
    }

    public void setBlockTimeStamp(int blockTimeStamp) {
        this.blockTimeStamp = blockTimeStamp;
    }

    public BigInteger getTickCumulative() {
        return tickCumulative;
    }

    public void setTickCumulative(BigInteger tickCumulative) {
        this.tickCumulative = tickCumulative;
    }

    public BigInteger getSecondsPerLiquidityCumulativeX128() {
        return secondsPerLiquidityCumulativeX128;
    }

    public void setSecondsPerLiquidityCumulativeX128(BigInteger secondsPerLiquidityCumulativeX128) {
        this.secondsPerLiquidityCumulativeX128 = secondsPerLiquidityCumulativeX128;
    }

    public Boolean getInitialized() {
        return initialized;
    }

    public void setInitialized(Boolean initialized) {
        this.initialized = initialized;
    }
}