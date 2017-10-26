package services

import java.io.IOException
import java.math.BigInteger
import java.util.{Collections, Optional}

import org.apache.commons.lang3.StringUtils
import org.web3j.abi.{FunctionEncoder, TypeReference}
import org.web3j.abi.datatypes.{Function, Uint}
import org.web3j.crypto.{Credentials, TransactionEncoder, WalletUtils}
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.{RawTransaction, Transaction}
import org.web3j.protocol.core.methods.response.{EthCall, EthSendTransaction, TransactionReceipt}
import org.web3j.tx.Transfer
import org.web3j.utils.Convert

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
  * Implementation of interaction with Ethereum API via RPC.
  *
  * @author Maxim Z.
  * @param web3j
  */
class EthereumService(web3j: Web3j) {

  def getBalance(addr: String): java.math.BigInteger = {
    web3j
      .ethGetBalance(addr, DefaultBlockParameterName.LATEST)
      .sendAsync()
      .get().getBalance
  }

  def sendEther(pwd: String, walletPath: String, destinationAddr: String, amount: java.math.BigDecimal): TransactionReceipt = Transfer.sendFunds(web3j, WalletUtils.loadCredentials(pwd, walletPath), destinationAddr, amount, Convert.Unit.ETHER)

  def sendTransactionReceiptRequest(transactionHash: String): Optional[TransactionReceipt] = {
    val transactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).send
    if (transactionReceipt.hasError) throw new IOException("Error processing request: " + transactionReceipt.getError.getMessage)
    transactionReceipt.getTransactionReceipt
  }

  def getCredential(walletPwd: String, walletPath: String): Credentials = WalletUtils.loadCredentials(walletPwd, walletPath)

  def deploySmartContract(contractBin: String, localAddr: String, credential: Credentials): EthSendTransaction = {
    val nonce = getNonce(localAddr)
    //TODO exclude static
    val rawTx = RawTransaction.createContractTransaction(nonce, BigInteger.valueOf(100000000000L), BigInteger.valueOf(4300000), BigInteger.ZERO, contractBin)
    val signedMessage = TransactionEncoder.signMessage(rawTx, credential)
    val hexValue = org.web3j.utils.Numeric.toHexString(signedMessage)
    web3j.ethSendRawTransaction(hexValue).sendAsync.get()
  }

  private def getNonce(addr: String): java.math.BigInteger = web3j.ethGetTransactionCount(addr, DefaultBlockParameterName.LATEST).sendAsync().get().getTransactionCount

  def getContractBin(path: String): String = {
    val source = scala.io.Source.fromFile(path)
    try source.mkString finally source.close()
  }

  def callSmartContractFunction(function: Function, from: String, contractAddress: String): Future[EthCall] = {
    val encodedFunction = FunctionEncoder.encode(function)
    val tx = Transaction.createEthCallTransaction(from, contractAddress, encodedFunction)
    Future {
      web3j.ethCall(tx, DefaultBlockParameterName.LATEST).sendAsync().get()
    }
  }

  //TODO ...
/*
  def createFunction(fName: String, params: java.util.List[String]): Unit ={
    StringUtils.isNumeric("fName")
    val f =  new Function(fName, Collections.singletonList(new Uint(BigInteger.valueOf(4))), Collections.singletonList(new TypeReference[Uint]() {}))
  }
*/

  def getTxReceipt(txHash: String): Optional[TransactionReceipt] = web3j.ethGetTransactionReceipt(txHash).sendAsync().get().getTransactionReceipt

  /**
    * @param txHash
    * @param attempts each of attempts takes 1 second
    * @return transaction receipt
    */
  @tailrec
  final def getTxReceipt(txHash: String, attempts: Int): Future[Option[TransactionReceipt]] = {
    if (attempts <= 0) return Future {
      Option.empty
    }
    val receipt = getTxReceipt(txHash)
    val optional = if (receipt.isPresent) Some(receipt.get()) else None
    optional match {
      case Some(rec) => Future {
        Option(rec)
      }
      case None => {
        Thread.sleep(1000)
        getTxReceipt(txHash, attempts - 1)
      }
    }
  }

}
