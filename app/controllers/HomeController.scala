package controllers

import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.ExecutionContext

/**
  * A very small controller that renders a home page.
  */
class HomeController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def index = Action { implicit request => Ok(views.html.index()) }

}




