## 1 项目环境配置
* 开发语言： Java
* IDE： idea
* 编译工具： gradle, 因为gradle写配置简洁些, gradle的包管理也依赖maven,因此**配置国内源**的方法跟maven一样\
   idea下gradle插件默认自带，也会自动下载gradle， Eclipse下不太清楚
* 基本技术栈： springBoot, web3j(以太坊RPC API的SDK), zksync-java2(zkSync RPC api的一些扩展)

## 2 程序配置
使用的springBoot的配置文件

### 2.1 公共配置application.properties

### 2.2 私有配置application-secret.properties
这个文件是本地的， 需要自己在resource目录手动创建，不会上传到代码仓库 \
助记词配置在这个文件中， 属性名称是：account.mnemonic 

## 3 代码
* App.java: 代码执行入口方法
* EvmService ZkSyncService: 区块链RPC API服务方法
* AccountUtils: 帐号有关的工具方法
* xxxTask： 常见的一些区块链交互任务

## 4 Solidity文件编译
### 4.1 Evm编译
./solc-static-linux --bin --abi -o output-dir input.sol

### 4.2 ZkSync编译
* 编译
```
~/opt/zksolc-linux-amd64-musl-v1.3.21 Blockcast.sol --solc ~/opt/solc-static-linux --combined-json abi,bin -O3 -o ./build --overwrite
```

* 提取出hex形式的合约字节码
```
cat build/combined.json | jq | sed -n -E 's/[ \t]*"bin": "([^"]*)",/\1/p' > build/contract_hex.txt
```

## 5 Geth
### 5.1 start geth
`./geth --dev --http`
* dev: developemnt mode
* --http: enable httpRPC

### 5.2 js console
`./geth attach /tmp/geth.ipc`

* 从coinbase账户转eth到测试账户 \
  `eth.sendTransaction({from: eth.coinbase, to: "0xdca2fa08273c795ac2356dc97bb887b7f2d49e5e", value: web3.toWei(30, "ether")})`

## debug
* 合约方法签名对应不上时（方法名或者参数列表） 会报reverted transaction错误， 但reverted transaction这个错误太普遍，可以是其他原因导致，如何获取到更准确的信息
* estimateGas的时候传递的Transaction对象最好是完整的，因为estimateGas会去尝试执行对应的合约方法，合约方法的执行情况可能会取决于传递的参数，比如参数检查，检查失败会revert
* getLogs方法中能否根据event中的第2个topic进行过滤？