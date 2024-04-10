#!/bin/awk

BEGIN {FS = ","}
NR == 1 {print "time,price" }
$1 > "2024-02-23T14:21:00Z[UTC]" && $1 < "2024-02-23T14:51:00Z[UTC]" && prevTime != substr($1, 1, 16) {
	time = substr($1, 1, 16)
	print time","$2
	prevTime=time
}
