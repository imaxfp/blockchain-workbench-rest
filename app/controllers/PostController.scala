package controllers

import javax.inject.Inject

import com.google.gson.Gson
import conf.PostControllerComponents
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
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
case class ChargeModel(toAddr: String, amount: BigDecimal, walletPath: String, pwd: String)

case class BalanceModel(addr: String, amount: java.math.BigInteger)

case class SmartContractModel(contractBin: String, addr: String, walletPath: String, pwd: String)

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

  val gson = new Gson()

  import play.api.data.Forms._

  val balanceMapping: Form[List[String]] = {
    Form(mapping(
      addr -> list(nonEmptyText)
    )(List.apply)(Option[List[String]]))
  }

  val hashesMapping: Form[List[String]] = {
    Form(mapping(
      hash -> list(nonEmptyText)
    )(List.apply)(Option[List[String]]))
  }

  val chargeMapping: Form[ChargeModel] = {
    Form(mapping(
      addr -> nonEmptyText,
      amount -> bigDecimal,
      walletPath -> nonEmptyText,
      pwd -> nonEmptyText
    )(ChargeModel.apply)(ChargeModel.unapply))
  }

  val smartContractMapping: Form[SmartContractModel] = {
    Form(mapping(
      contractBin -> nonEmptyText,
      addr -> nonEmptyText,
      walletPath -> nonEmptyText,
      pwd -> nonEmptyText
    )(SmartContractModel.apply)(SmartContractModel.unapply))
  }

}

object BlockchainConnectors {
  val web3j = Web3j.build(new HttpService("http://127.0.0.1:40403/"))
  val ethApiService = new EthereumService(web3j)
}


/**
  * Takes HTTP requests and produces JSON.
  */
class PostController @Inject()(cc: PostControllerComponents)(implicit ec: ExecutionContext) extends BaseController with SimpleRouter {

  import BlockchainConnectors._
  import CustomJsonMapper._

  private val logger = Logger(getClass)

  override protected def controllerComponents: PostControllerComponents = cc


  def ethBalance(in: List[String]): Future[Result] = response(Map("balances" -> in.map(a => BalanceModel(a, ethApiService.getBalance(a))).asJava))

  def ethCharge(in: ChargeModel): Future[Result] = response(Map("txHash" -> ethApiService.sendEther(in.pwd, in.walletPath, in.toAddr, in.amount.bigDecimal).getTransactionHash))

  def ethContractDeploy(in: SmartContractModel): Future[Result] = response(Map("fullHash" -> ethApiService.deploySmartContract(in.contractBin, in.addr, ethApiService.getCredential(in.pwd, in.walletPath)).getResult))

  def ethContractInfo(in: List[String]): Future[Result] = {
    val r = in.map(a => Map(a -> ethApiService.getTxReceipt(a).get().getContractAddress))
    response(Map("contractInfo" -> r))
  }

  def ethContractRun(in: SmartContractModel): Future[Result] = response(Map("fullHash" -> ethApiService.deploySmartContract(in.contractBin, in.addr, ethApiService.getCredential(in.pwd, in.walletPath)).getResult))


  def response[T](o: T): Future[Result] = Future(Created(gson.toJson(o)))


  override def routes: Routes = {
    case POST(p"/eth/balance") => process(balanceMapping, ethBalance)
    case POST(p"/eth/charge") => process(chargeMapping, ethCharge)
    case POST(p"/eth/deploy") => process(smartContractMapping, ethContractDeploy)
    case POST(p"/eth/contract/info") => process(hashesMapping, ethContractInfo)
    case POST(p"/eth/contract/run") => process(hashesMapping, ethContractInfo)
  }

  def process[T](form: Form[T], success: T => Future[Result]): Action[AnyContent] = controllerComponents.PostAction.async { implicit request =>

    logger.trace("process ... : " + request.body.asJson)

    def failureJsonReading(badForm: Form[T]) = Future.successful(BadRequest(badForm.errorsAsJson))

    form.bindFromRequest().fold(failureJsonReading, success)
  }

}


