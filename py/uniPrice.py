#!/bin/python3

import matplotlib.pyplot as plt
import matplotlib as mpl
from matplotlib.dates import DateFormatter

import numpy as np

import csv

def plotFromFile(fn, ax, color, label):
    times=[]
    prices=[]
    with open(fn, 'r') as f:
        reader = csv.reader(f)
        # skip title
        reader.__next__()
        for row in reader:
            time=row[0]
            prices.append(float(row[1]))
            times.append(np.datetime64(time))
    ax.plot(times, prices, color=color, linestyle='dashed', label=label)
    ax.scatter(times, prices, c=color)


#fig, ax = plt.subplots()  # Create a figure containing a single axes.
fig, ax = plt.subplots(layout='constrained')
ax.set_title('UNI prices from ChainLink and Uniswap TWAP')
ax.set_xlabel('Time')
ax.set_ylabel('Price(USD)')
ax.xaxis.set_major_formatter(DateFormatter('%H:%M'))
ax.set_ylim(5, 13)

data_x = [np.datetime64('2024-02-23T14:20'), np.datetime64('2024-02-23T14:21'),np.datetime64('2024-02-23T14:22'),np.datetime64('2024-02-23T14:23')]
#ax.plot(data_x, [7.11, 7.33, 8.15, 9.34])  # Plot some data on the axes.
#ax.plot(data_x, [7.12, 8.36, 9.10, 10.68])  # Plot some data on the axes.
#ax.scatter(data_x, [1, 4, 2, 3], c='green')  # Plot some data on the axes.

plotFromFile('data/chainLinkPrice.csv', ax, 'green', 'chainLink')
plotFromFile('data/uniswapPrice_12.csv', ax, 'blue', 'uniswap period=12 seconds')
plotFromFile('data/uniswapPrice_900.csv', ax, 'orange', 'uniswap period=15 minutes')
plotFromFile('data/uniswapPrice_1800.csv', ax, 'red', 'uniswap period=30 minutes')

ax.legend()

plt.show()
