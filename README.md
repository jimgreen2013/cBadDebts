Source Code for [my indpendent invetigation report of Compound Bad Debts](https://blog.songziyu.cc/7.html)

In order to verify the reason of the incident, we need to query lots of data 
through the Ethereum RPC api:
* Financial state of bad debts accounts on Compound
* parameters of Compound protocol, for example, `collateral factor`, 
  `lowerBoundAnchorRatio`, `upperBoundAnchorRatio`, `anchorPeriod`, etc.
* verify the oracle that was being used during the incident
* price events(`priceUpdate`, `priceGuarded`) during the incident
* Uniswap oracle data(`observation`) for TWAP analysis

[Web3j](https://github.com/web3j/web3j) is used for Ethereum RPC, the 
implementation details is as follow:
* `EvmService.java` general Ethereum RPC 
* `CompoundService` Compound related functions
* `UniService` Uniswap related functions
* `py` foloder is code for plotting figures with [matplot](https://matplotlib.org)\
   awk is also used for csv data processing which is really neat and cool!


