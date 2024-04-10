#!/bin/awk

BEGIN {FS = ","}
NR == 1 {printf("time,price\n")}
$8 == "cUNI" && substr($2, 1, 16) != prevTime {
	split($6, prices, " ")
	price = prices[1]/1e6
	if (price < 10)
		price = substr(prices[1]/1e6, 1, 4)
	else
		price = substr(prices[1]/1e6, 1, 5)
	time=substr($2, 1, 16)

	printf("%s,%s\n", time, price)
	prevTime=time
}
