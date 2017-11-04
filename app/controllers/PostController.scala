package controllers

import javax.inject.Inject

import com.google.gson.Gson
import conf.PostControllerComponents
import org.web3j.protocol.core.methods.response.TransactionReceipt
import play.api.Logger
import play.api.data.Form
import play.api.mvc._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import services.EthereumService

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.existentials

/**
  * @author Maxim Z.
  *         Implementation of asynchronous post controller.
  */
case class ChargeModel(toAddr: String, amount: BigDecimal, walletPath: String, pwd: String, rpcNodes: List[String])

case class MultiChargeModel(addr: String, amount: BigDecimal, walletPath: String, pwd: String, countCharge: Int, timeoutMs: Long, rpcNodes: List[String])

case class BalanceModel(addr: String, amount: java.math.BigInteger)

case class Balances(addr: List[String], rpcNodes: List[String])

case class ContractInfo(hash: String, rpcNode: String)

case class SmartContractModel(contractBin: String, addr: String, walletPath: String, pwd: String, rpcNodes: List[String])

case class ExecuteContractMethodModel(from: String, contractAddress: String, functionName: String, parameter: String, rpcNodes: List[String])

object CustomJsonMapper {

  /**
    * JSON fields
    */
  val addr = "addr"
  val hash = "hashes"
  val amount = "amount"
  val walletPath = "walletPath"
  val pwd = "pwd"

  val contractBin = "contractBin"

  val from = "from"
  val contractAddr = "contractAddr"
  val functionName = "functionName"
  val functionParams = "functionParams"
  val countCharge = "countCharge"
  val timeoutMs = "timeoutMs"
  val rpcNodes = "rpcNodes"


  val gson = new Gson()

  import play.api.data.Forms._

  val balanceMapping: Form[Balances] = {
    Form(mapping(
      addr -> list(nonEmptyText),
      rpcNodes -> list(nonEmptyText)
    )(Balances.apply)(Balances.unapply))
  }

  val hashesMapping: Form[ContractInfo] = {
    Form(mapping(
      hash -> nonEmptyText,
      rpcNodes -> nonEmptyText
    )(ContractInfo.apply)(ContractInfo.unapply))
  }

  val executeContractMapping: Form[ExecuteContractMethodModel] = {
    Form(mapping(
      from -> nonEmptyText,
      contractAddr -> nonEmptyText,
      functionName -> nonEmptyText,
      functionParams -> nonEmptyText,
      rpcNodes -> list(nonEmptyText)
    )(ExecuteContractMethodModel.apply)(ExecuteContractMethodModel.unapply))
  }

  val chargeMapping: Form[ChargeModel] = {
    Form(mapping(
      addr -> nonEmptyText,
      amount -> bigDecimal,
      walletPath -> nonEmptyText,
      pwd -> nonEmptyText,
      rpcNodes -> list(nonEmptyText)
    )(ChargeModel.apply)(ChargeModel.unapply))
  }

  val multiChargeMapping: Form[MultiChargeModel] = {
    Form(mapping(
      addr -> nonEmptyText,
      amount -> bigDecimal,
      walletPath -> nonEmptyText,
      pwd -> nonEmptyText,
      countCharge -> number,
      timeoutMs -> longNumber,
      rpcNodes -> list(nonEmptyText)
    )(MultiChargeModel.apply)(MultiChargeModel.unapply))
  }

  val smartContractMapping: Form[SmartContractModel] = {
    Form(mapping(
      contractBin -> nonEmptyText,
      addr -> nonEmptyText,
      walletPath -> nonEmptyText,
      pwd -> nonEmptyText,
      rpcNodes -> list(nonEmptyText)
    )(SmartContractModel.apply)(SmartContractModel.unapply))
  }

}

/**
  * Takes HTTP requests and produces JSON.
  */
class PostController @Inject()(cc: PostControllerComponents)(implicit ec: ExecutionContext) extends BaseController with SimpleRouter {

  val ethApiService = new EthereumService()

  import CustomJsonMapper._

  private val logger = Logger(getClass)

  override protected def controllerComponents: PostControllerComponents = cc

  def ethBalance(in: Balances): Future[Result] = {
    val rpcNodeUrl = in.rpcNodes.head
    response(Map("balances" -> in.addr.map(a => BalanceModel(a, ethApiService.getBalance(a, rpcNodeUrl))).asJava))
  }

  def ethCharge(in: ChargeModel): Future[Result] = response(Map("txHash" -> ethApiService.sendEther(in.pwd, in.walletPath, in.toAddr, in.amount.bigDecimal, in.rpcNodes.head).getTransactionHash))

  def ethMultiCharge(in: MultiChargeModel): Future[Result] = response(Map("txHash" -> ethApiService.sendMultiChargeWithDelay(in)))

  def ethContractDeploy(in: SmartContractModel): Future[Result] = response(Map("fullHash" -> ethApiService.deploySmartContract(in.contractBin, in.addr, ethApiService.getCredential(in.pwd, in.walletPath), in.rpcNodes.head).getResult))

  def ethContractInfo(in: ContractInfo): Future[Result] = {
    response(Map(in -> ethApiService.getTxReceipt(in.hash, ethApiService.getConnector(in.rpcNode)).orElse(new TransactionReceipt()).getContractAddress))
  }

  def ethContractRunDouble(in: ExecuteContractMethodModel): Future[Result] = response(Map("result" -> ethApiService.runDouble(in.functionName, in.from, in.contractAddress, in.parameter, in.rpcNodes.head)))


  def response[T](o: T): Future[Result] = Future(Created(gson.toJson(o)))


  override def routes: Routes = {
    case POST(p"/eth/balance") => process(balanceMapping, ethBalance)
    case POST(p"/eth/charge") => process(chargeMapping, ethCharge)
    case POST(p"/eth/deploy") => process(smartContractMapping, ethContractDeploy)
    case POST(p"/eth/contract/info") => process(hashesMapping, ethContractInfo)
    case POST(p"/eth/contract/run") => process(executeContractMapping, ethContractRunDouble)
    case POST(p"/eth/multicharge") => process(multiChargeMapping, ethMultiCharge)
  }

  def process[T](form: Form[T], success: T => Future[Result]): Action[AnyContent] = controllerComponents.PostAction.async { implicit request =>
    logger.trace("process ... : " + request.body.asJson)

    def failureJsonReading(badForm: Form[T]) = Future.successful(BadRequest(badForm.errorsAsJson))

    form.bindFromRequest().fold(failureJsonReading, success)
  }

}


