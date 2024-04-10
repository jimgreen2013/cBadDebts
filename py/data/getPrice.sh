awk -f getChainLinkPrice.awk com_uniswapAnchoredView_events_decoded.csv > chainLinkPrice.csv
awk -f getUniswapPrice.awk uni_price_900_all.csv > uniswapPrice_900.csv
awk -f getUniswapPrice.awk uni_price_1800_all.csv > uniswapPrice_1800.csv
awk -f getUniswapPrice.awk uni_price_12_all.csv > uniswapPrice_12.csv
