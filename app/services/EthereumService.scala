package services

import java.io.IOException
import java.math.BigInteger
import java.util.{Collections, Optional}

import controllers.MultiChargeModel
import org.web3j.abi.datatypes.{Function, Uint}
import org.web3j.abi.{FunctionEncoder, FunctionReturnDecoder, TypeReference}
import org.web3j.crypto.{Credentials, TransactionEncoder, WalletUtils}
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.{RawTransaction, Transaction}
import org.web3j.protocol.core.methods.response.{EthCall, EthSendTransaction, TransactionReceipt}
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import play.api.libs.json.JsResult.Exception

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, _}

/**
  * Implementation of interaction with Ethereum API via RPC.
  *
  * @author Maxim Z.
  */
class EthereumService() {

  def getConnector(url: String) = Web3j.build(new HttpService(url))

  def getBalance(addr: String, nodeUrl: String): java.math.BigInteger = {
    getConnector(nodeUrl)
      .ethGetBalance(addr, DefaultBlockParameterName.LATEST)
      .sendAsync()
      .get().getBalance
  }

  def sendEther(pwd: String, walletPath: String, destinationAddr: String, amount: java.math.BigDecimal, nodeUrl: String): TransactionReceipt = Transfer.sendFunds(getConnector(nodeUrl), WalletUtils.loadCredentials(pwd, walletPath), destinationAddr, amount, Convert.Unit.ETHER)

  def sendMultiChargeWithDelay(mCharge: MultiChargeModel): Unit = {
    val amount = mCharge.amount
    mCharge.rpcNodes.foreach(url => {
      val web3jLocal = getConnector(url)
      for (i <- 1 to mCharge.countCharge) {
        new Thread(new Runnable {
          def run() {
            Transfer.sendFunds(web3jLocal, WalletUtils.loadCredentials(mCharge.pwd, mCharge.walletPath), mCharge.addr, amount.bigDecimal, Convert.Unit.ETHER)
          }
        }).start()
        Thread.sleep(mCharge.timeoutMs)
      }
    })
  }

  def sendTransactionReceiptRequest(transactionHash: String, nodeUrl: String): Optional[TransactionReceipt] = {
    val transactionReceipt = getConnector(nodeUrl).ethGetTransactionReceipt(transactionHash).send
    if (transactionReceipt.hasError) throw new IOException("Error processing request: " + transactionReceipt.getError.getMessage)
    transactionReceipt.getTransactionReceipt
  }

  def getCredential(walletPwd: String, walletPath: String): Credentials = WalletUtils.loadCredentials(walletPwd, walletPath)

  def deploySmartContract(contractBin: String, localAddr: String, credential: Credentials, nodeUrl: String): EthSendTransaction = {
    val nonce = getNonce(localAddr, nodeUrl)
    //TODO will add property as parameters. Exclude static
    val rawTx = RawTransaction.createContractTransaction(nonce, BigInteger.valueOf(100000000000L), BigInteger.valueOf(4300000), BigInteger.ZERO, contractBin)
    val signedMessage = TransactionEncoder.signMessage(rawTx, credential)
    val hexValue = org.web3j.utils.Numeric.toHexString(signedMessage)
    getConnector(nodeUrl).ethSendRawTransaction(hexValue).sendAsync.get()
  }

  private def getNonce(addr: String, nodeUrl: String): java.math.BigInteger = getConnector(nodeUrl).ethGetTransactionCount(addr, DefaultBlockParameterName.LATEST).sendAsync().get().getTransactionCount

  def getContractBin(path: String): String = {
    val source = scala.io.Source.fromFile(path)
    try source.mkString finally source.close()
  }

  def callSmartContractFunction(function: Function, from: String, contractAddress: String, nodeUrl: String): Future[EthCall] = {
    val encodedFunction = FunctionEncoder.encode(function)
    val tx = Transaction.createEthCallTransaction(from, contractAddress, encodedFunction)
    Future {
      getConnector(nodeUrl).ethCall(tx, DefaultBlockParameterName.LATEST).sendAsync().get()
    }
  }

  def runDouble(funName: String, from: String, contractAddress: String, parameter: String, nodeUrl: String): String = {
    val param = java.lang.Long.parseLong(parameter, 10)
    val doubledFunction = new Function(funName, Collections.singletonList(new Uint(BigInteger.valueOf(param))), Collections.singletonList(new TypeReference[Uint]() {}))
    val encodedFunction = FunctionEncoder.encode(doubledFunction)
    val tx = Transaction.createEthCallTransaction(from, contractAddress, encodedFunction)
    val res = getConnector(nodeUrl).ethCall(tx, DefaultBlockParameterName.LATEST).sendAsync().get()
    val resultList = FunctionReturnDecoder.decode(res.getValue, doubledFunction.getOutputParameters)
    val r = resultList.get(0).getValue
    r.toString
  }

  def getTxReceipt(txHash: String, web3j: Web3j): Optional[TransactionReceipt] = {
    try {
      web3j.ethGetTransactionReceipt(txHash).sendAsync().get().getTransactionReceipt
    } catch {
      case e: Exception => Optional.empty()
    }
  }

  /**
    * @param txHash
    * @param attempts each of attempts takes 1 second
    * @return transaction receipt
    */
  @tailrec
  final def getTxReceipt(txHash: String, attempts: Int, web3j: Web3j): Future[Option[TransactionReceipt]] = {
    if (attempts <= 0) return Future {
      Option.empty
    }
    val receipt = getTxReceipt(txHash, web3j)
    val optional = if (receipt.isPresent) Some(receipt.get()) else None
    optional match {
      case Some(rec) => Future {
        Option(rec)
      }
      case None => {
        Thread.sleep(1000)
        getTxReceipt(txHash, attempts - 1, web3j)
      }
    }
  }

}
