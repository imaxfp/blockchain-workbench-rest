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


request:
```json
{   
	"addr":["0xb501199150824804ba902e57d2eb92170150735c","0xcd7ab062ce3ff7bc8d91397df731f3b74bcbed01"]
}
```

#### Charge account
```json
{   
	"walletPath":"/path/localNode/keystore/UTC--2017-10-18T06-43-08.682098976Z--b501199150824804ba902e57d2eb92170150735c",
	"pwd":"yourpass",
	"addr":"0xcd7ab062ce3ff7bc8d91397df731f3b74bcbed01",
	"amount":10
}
```

#### Deploy smart contract and get transaction hash
http://localhost:9000/v1/eth/deploy

```json
{   
	"walletPath":"/path/localNode/keystore/UTC--2017-10-18T06-43-08.682098976Z--b501199150824804ba902e57d2eb92170150735c",
    "pwd":"yourpass",
    "addr":"0xb501199150824804ba902e57d2eb92170150735c",
	"contractBin":"6060604..."
}
```

#### Deploy smart contract and get contract address by transaction hash after confirmations
http://localhost:9000/v1/eth/contract/info

```json
{   
	"hashes":["0x9f53279a33e2e5afa51eb91bf5598af5246fffd1997d917bf6b556627ed473a8", "..."]
}
```

#### Execute method on the deployed smart contract 





   


