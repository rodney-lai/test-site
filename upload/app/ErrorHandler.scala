
import play.api.http.{HttpErrorHandler,Status}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.util._

class ErrorHandler @Inject() (globalHelper:GlobalHelper) extends HttpErrorHandler {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  def onClientError(request: RequestHeader, statusCode: Int, errorMessage: String) = {
    statusCode match {
      case Status.BAD_REQUEST => for {
        _ <- globalHelper.onBadRequestMsg(request,errorMessage)
      } yield BadRequest(Json.toJson("fail"))
      case Status.NOT_FOUND => for {
        _ <- globalHelper.onHandlerNotFoundMsg(request)
      } yield NotFound(Json.toJson("fail"))
      case _ => for {
        _ <- globalHelper.onClientErrorMsg(request,statusCode,errorMessage)
      } yield InternalServerError(Json.toJson("fail"))
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    for {
      _ <- globalHelper.onErrorMsg(request,exception)
    } yield InternalServerError(Json.toJson("fail"))
  }
}
