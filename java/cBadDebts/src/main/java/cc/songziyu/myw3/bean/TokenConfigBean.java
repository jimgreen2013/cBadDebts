package cc.songziyu.myw3.bean;

import java.math.BigInteger;

public class TokenConfigBean {
    private String symbol;
    private String cToken;
    private String underlying;
    private String symbolHash;
    private BigInteger baseUint;
    private Integer priceSource;
    private BigInteger fixedPrice;
    private String uniswapMarket;
    private String reporter;
    private BigInteger reporterMultiplier;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getcToken() {
        return cToken;
    }

    public void setcToken(String cToken) {
        this.cToken = cToken;
    }

    public String getUnderlying() {
        return underlying;
    }

    public void setUnderlying(String underlying) {
        this.underlying = underlying;
    }

    public String getSymbolHash() {
        return symbolHash;
    }

    public void setSymbolHash(String symbolHash) {
        this.symbolHash = symbolHash;
    }

    public BigInteger getBaseUint() {
        return baseUint;
    }

    public void setBaseUint(BigInteger baseUint) {
        this.baseUint = baseUint;
    }

    public Integer getPriceSource() {
        return priceSource;
    }

    public void setPriceSource(Integer priceSource) {
        this.priceSource = priceSource;
    }

    public BigInteger getFixedPrice() {
        return fixedPrice;
    }

    public void setFixedPrice(BigInteger fixedPrice) {
        this.fixedPrice = fixedPrice;
    }

    public String getUniswapMarket() {
        return uniswapMarket;
    }

    public void setUniswapMarket(String uniswapMarket) {
        this.uniswapMarket = uniswapMarket;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public BigInteger getReporterMultiplier() {
        return reporterMultiplier;
    }

    public void setReporterMultiplier(BigInteger reporterMultiplier) {
        this.reporterMultiplier = reporterMultiplier;
    }
}
