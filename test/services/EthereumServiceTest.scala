package services

import java.math.BigInteger
import java.util.Collections
import controllers.MultiChargeModel
import junit.framework.TestCase.assertFalse
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.web3j.abi.datatypes.{Function, Uint}
import org.web3j.abi.{FunctionReturnDecoder, TypeReference}
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

import scala.collection.JavaConversions._

class EthereumServiceIT extends FunSuite with ScalaFutures {

  /**
    * Smart contract functions
    */
  private def fibonacciFunction = new Function("fibonacciNotify", Collections.singletonList(new Uint(BigInteger.valueOf(4))), Collections.singletonList(new TypeReference[Uint]() {}))

  /**
    * Wallet, addresses and credentials
    */
  val walletPath = "/Users/maxim/DA/DASPilot/das_net/node3/keystore/UTC--2017-10-18T06-43-08.682098976Z--b501199150824804ba902e57d2eb92170150735c"
  val localAddress = "0xb501199150824804ba902e57d2eb92170150735c"
  val remoteAddress = "0xcd7ab062ce3ff7bc8d91397df731f3b74bcbed01"
  val walletPwd = "testpass"
  val credentials = WalletUtils.loadCredentials(walletPwd, walletPath)

  val ethApiService = new EthereumService()
  val nodeURL = "http://127.0.0.1:40403/"
  val web3j = Web3j.build(new HttpService(nodeURL))

  /**
    * Smart contracts in binary mode. solc -o outputDirectory --bin --ast --asm sourceFile.sol
    */
  val contractFibonacciBin = ethApiService.getContractBin("./resources/solidity/Fibonacci.bin")

  test("Get balances") {
    println(localAddress + " = " + ethApiService.getBalance(localAddress, nodeURL))
    println(remoteAddress + " = " + ethApiService.getBalance(remoteAddress, nodeURL))
  }

  test("Single charge address with Ethereum wallet file 'recommended'") {
    println("Before charge = " + ethApiService.getBalance(remoteAddress, nodeURL))
    ethApiService.sendEther(walletPwd, walletPath, remoteAddress, java.math.BigDecimal.valueOf(777), nodeURL)
    println("After charge =" + ethApiService.getBalance(remoteAddress, nodeURL))
  }

  test("Multi charge address with Ethereum wallet file 'recommended'") {
    println("Before charge = " + ethApiService.getBalance(remoteAddress, nodeURL))
    val multiCharge = MultiChargeModel(remoteAddress, 1, walletPath, walletPwd, 2, 150, List("http://127.0.0.1:40403/"))
    ethApiService.sendMultiChargeWithDelay(multiCharge)
    //Thread.sleep(multiCharge.countCharge * multiCharge.timeoutMs + 1)
    Thread.sleep(10000)
  }

  test("Deploy smart contract and get transaction hash") {
    val tx = ethApiService.deploySmartContract(contractFibonacciBin, localAddress, ethApiService.getCredential(walletPwd, walletPath), nodeURL)
    assertFalse(tx.getTransactionHash.isEmpty)
    println("Transaction hash = " + tx.getTransactionHash)
  }

  /**
    * Receiving transaction receipt after submitting transaction.
    * Can take different time, according to blockchain network performance
    */
  test("Deploy smart contract and get contract address by transaction hash after confirmations") {
    val tx = ethApiService.deploySmartContract(contractFibonacciBin, localAddress, ethApiService.getCredential(walletPwd, walletPath), nodeURL)
    assertFalse(tx.getTransactionHash.isEmpty)
    whenReady(ethApiService.getTxReceipt(tx.getTransactionHash, 100, web3j)) { addr =>
      assertFalse(addr.isEmpty)
      println("contract address = " + addr.get.getContractAddress)
    }
  }


  test("Deploy smart contract and execute smart contract method") {
    val tx = ethApiService.deploySmartContract(contractFibonacciBin, localAddress, ethApiService.getCredential(walletPwd, walletPath), nodeURL)
    assertFalse(tx.getTransactionHash.isEmpty)
    whenReady(ethApiService.getTxReceipt(tx.getTransactionHash, 100, web3j)) { addr =>
      assertFalse(addr.isEmpty)
      whenReady(ethApiService.callSmartContractFunction(fibonacciFunction, localAddress, addr.get.getContractAddress, nodeURL)) { res =>
        val resultList = FunctionReturnDecoder.decode(res.getValue, fibonacciFunction.getOutputParameters)
        resultList.toList.foreach(r => println(r.getValue))
      }
    }
  }

  private def doubledFunction = new Function("doubleNumber", Collections.singletonList(new Uint(BigInteger.valueOf(2))), Collections.singletonList(new TypeReference[Uint]() {}))

  test("Run contract method after contract installation") {
    whenReady(ethApiService.callSmartContractFunction(doubledFunction, localAddress, "0x16a2d105e89bfc1c4fa672993cab876acd858092", nodeURL)) { res =>
      val resultList = FunctionReturnDecoder.decode(res.getValue, fibonacciFunction.getOutputParameters)
      resultList.toList.foreach(r => println(r.getValue))
    }
  }

}
