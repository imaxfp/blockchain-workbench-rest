## Welcome to the workbench blockchain REST service. This is asynchronous REST API, based on play framework with akka.     
 
This workbench was created for measurement and comparison performance between different blockchain networks.
Currently supports Ethereum. 

In the future next blockhain networks will be added:

* Hyperladger 
* Iroha
* ...

### Setup your private Ethereum network
Please use the next detailed manual - https://github.com/imaxfp/eth-setup-scripts

### Deploy and run app from source
* Please make sure if you have installed SBT 0.13.15 or older
* git clone https://github.com/imaxfp/blockchain-workbench-rest
* cd blockchain-workbench-rest
* sbt run

```bash
--- (Running the application, auto-reloading is enabled) ---

[info] p.c.s.AkkaHttpServer - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

(Server started, use Enter to stop and go back to the console...)

```

### REST API calls by example:
Please be aware, it is not save to send your credential data to the remote node. 
In case with production network you should run and connect to the node node locally.  

#### Check balances
http://localhost:9000/v1/eth/balance

```json
{   
	"addr":["0xb501199150824804ba902e57d2eb92170150735c","0xcd7ab062ce3ff7bc8d91397df731f3b74bcbed01"],
	"rpcNodes":["http://127.0.0.1:40403/"]
}
```

#### Charge account
http://localhost:9000/v1/eth/charge

```json
{   
	"walletPath":"/path/keystore/UTC--2017-10-18T06-43-08.682098976Z--b501199150824804ba902e57d2eb92170150735c",
	"pwd":"testpass",
	"addr":"0xcd7ab062ce3ff7bc8d91397df731f3b74bcbed01",
	"amount":100,
	"rpcNodes":["http://127.0.0.1:40403/"]
}

```

#### Multi charge account 
http://localhost:9000/v1/eth/multicharge

```json
{   
	"addr":"0xcd7ab062ce3ff7bc8d91397df731f3b74bcbed01",
	"amount":1,
	"walletPath":"/path/keystore/UTC--2017-10-18T06-43-08.682098976Z--b501199150824804ba902e57d2eb92170150735c",
	"pwd":"testpass",
	"countCharge":2,
	"timeoutMs":150,
	"rpcNodes":["http://127.0.0.1:40403/","http://127.0.0.1:40403/"]
}
```

#### Deploy smart contract and get transaction hash
http://localhost:9000/v1/eth/deploy

```json
{   
	"walletPath":"/path/keystore/UTC--2017-10-18T06-43-08.682098976Z--b501199150824804ba902e57d2eb92170150735c",
	"pwd":"testpass",
	"addr":"0xb501199150824804ba902e57d2eb92170150735c",
	"rpcNodes":["http://127.0.0.1:40403/"],
	"contractBin":"60606..."
}
```

#### Deploy smart contract and get contract address by transaction hash after confirmations
http://localhost:9000/v1/eth/contract/info

```json
{   
	"hashes":"0x711d7d005adecc88571551c7b38df90d3f3d6c4298ad494e148b821eda92ad5d",
	"rpcNodes":"http://127.0.0.1:40403/"
}
```

#### Execute method on the deployed smart contract 
http://localhost:9000/v1/eth/contract/run

```json
{   
	"from":"0xb501199150824804ba902e57d2eb92170150735c",
	"contractAddr":"0x88071a3172731762806c10a6a5bae1a364493195",
	"functionName":"doubleNumber",
	"functionParams":7,
	"rpcNodes":["http://127.0.0.1:40403/"]
}
```



   


