/**
 *
 * Copyright (c) 2015 Rodney S.K. Lai
 *
 * Permission to use, copy, modify, and/or distribute this software for 
 * any purpose with or without fee is hereby granted, provided that the 
 * above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES 
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR 
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES 
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN 
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF 
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package controllers.services

import play.api.libs.json._
import play.api.mvc._
import scala.annotation.meta.field
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure, Success, Try}
import com.mongodb._
import com.wordnik.swagger.annotations._
import jp.t2v.lab.play2.auth._
import org.apache.log4j.Logger
import com.rodneylai.auth._
import com.rodneylai.models.mongodb._
import com.rodneylai.util._

@ApiModel("LoginModel")
case class LoginModel(@(ApiModelProperty @field)(position=1,required=true)login: String,
                      @(ApiModelProperty @field)(position=2,required=true)password: String)

@ApiModel("JoinModel")
case class JoinModel( @(ApiModelProperty @field)(position=1,required=true)full_name: String,
                      @(ApiModelProperty @field)(position=2,required=true)email: String,
                      @(ApiModelProperty @field)(position=3,required=true)user_name: String,
                      @(ApiModelProperty @field)(position=4,required=true)password: String)

@Api(value = "/auth", description = "authentication services")
class auth extends Controller with LoginLogout with AuthConfigImpl {

  private val m_log:Logger = Logger.getLogger(this.getClass.getName)
  private val m_validateEmailAddressRegEx:String = """[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]+"""
  private val m_validateFriendlyUrlRegEx:String = """^(?=.{3,}$)(?!-)(?!.*--)[a-z0-9-]*(?<!-)"""
  private val m_validatePasswordRegEx:String = """^(?=.{8,}$)(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).*"""

  implicit val joinModelFormat = Json.format[JoinModel]

  private def validateLogin(json:JsValue):Option[(java.util.UUID,Set[String])] = {

    ((json \ "login").asOpt[String],(json \ "password").asOpt[String]) match {
      case (Some(login),Some(password)) if (login.contains("@")) => {
        UserAccount.findByEmailAddress(login) match {
          case Some(userAccount) if (AuthHelper.validatePassword(password,userAccount.passwordHash)) => Some((userAccount.userUuid,userAccount.roleList))
          case _ => None
        }
      }
      case (Some(login),Some(password)) if (!login.contains("@")) => {
        UserAccount.findByFriendlyUrl(login) match {
          case Some(userAccount) if (AuthHelper.validatePassword(password,userAccount.passwordHash)) => Some((userAccount.userUuid,userAccount.roleList))
          case _ => None
        }
      }
      case _ => None
    }
  }

  def loginUserSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future(Ok(Json.toJson(controllers.routes.Application.index.url)))
  }

  def loginDeveloperSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future(Ok(Json.toJson(controllers.routes.Developer.index.url)))
  }

  def loginAdminSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future(Ok(Json.toJson(controllers.routes.Admin.index.url)))
  }

  @ApiOperation(value = "login", notes = "returns login redirect url", nickname="login", response = classOf[String], httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "login credentials", required = true, dataType = "controllers.services.LoginModel", paramType = "body")))
  def login = Action.async { implicit request =>
    request.body match {
      case AnyContentAsJson(json) => validateLogin(json) match {
        case Some((userUuid,roleList)) => {
          if (roleList.contains("developer")) {
            gotoLoginSucceeded(userUuid, loginDeveloperSucceeded(request))
          } else if (roleList.contains("admin")) {
            gotoLoginSucceeded(userUuid, loginAdminSucceeded(request))
          } else {
            gotoLoginSucceeded(userUuid, loginUserSucceeded(request))
          }
        }
        case None => Future(Forbidden(Json.toJson("fail")))
      }
      case _ => Future(Forbidden(Json.toJson("fail")))
    }
  }

  @ApiOperation(value = "join", notes = "returns join status", nickname="join", response = classOf[String], httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "join information", required = true, dataType = "controllers.services.JoinModel", paramType = "body")))
  def join = Action.async { implicit request =>
    if (play.api.Play.isDev(play.api.Play.current)) {
      Thread.sleep(2000)
    }
    request.body match {
      case AnyContentAsJson(json) => {
        json.validate[JoinModel] map {
          case join => {
            val errors:Seq[String] = Seq(
              if (join.full_name.trim.isEmpty) {
                Some("Full name is required.")
              } else {
                None
              },
              if (join.email.trim.isEmpty) {
                Some("Email address is required.")
              } else if (!join.email.trim.matches(m_validateEmailAddressRegEx)) {
                Some("Email address is not valid.")
              } else if (!UserAccount.findByEmailAddress(join.email.trim).isEmpty) {
                Some("Email address is already being used by another account.")
              } else {
                None
              },
              if (join.user_name.trim.isEmpty) {
                Some("User name is required.")
              } else if (!join.user_name.trim.matches(m_validateFriendlyUrlRegEx)) {
                Some("User name is not valid.  User name MUST be at least three characters and can consist of lower case letters, digits, and dashes ONLY.  User name cannot start or end with a dash and there cannot be two consecutive dashes.")
              } else if (!UserAccount.findByFriendlyUrl(join.user_name.trim).isEmpty) {
                Some("User Name is already being used by another account.")
              } else {
                None
              },
              if (join.password.trim.isEmpty) {
                Some("Password is required.")
              } else if (!join.password.trim.matches(m_validatePasswordRegEx)) {
                Some("Password is not valid.  Password MUST be at least eight characters and contain at least one uppercase letter, one lowercase letter, and one digit.")
              } else {
                None
              }
            ).flatten

            if (errors.length == 0) {
              if (MongoHelper.isActive) {
                val now:java.util.Date = java.util.Calendar.getInstance.getTime
                val userAccount:UserAccount = UserAccount(None,
                                                          java.util.UUID.randomUUID,
                                                          AuthHelper.hashPassword(join.password.trim),
                                                          join.email.trim,
                                                          join.email.trim.toLowerCase,
                                                          join.full_name.trim,
                                                          join.user_name.trim,
                                                          Set[String](),
                                                          "unconfirmed",
                                                          now,
                                                          now)

                Try(UserAccount.getCollection.insert(UserAccountMap.toBson(userAccount))) match {
                  case Success(writeResult) => Future(Ok(Json.toJson("okay")))
                  case Failure(ex) => {
                    ExceptionHelper.log(this.getClass,ex,Some("Failed to insert UserAccount"),None,Some(request))
                    Future(Ok(Json.toJson("fail")))
                  }
                }
              } else {
                Future(BadRequest(Json.toJson(Seq[String]("Datastore NOT configured.  User not created.  Try again later."))))
              }
            } else {
              Future(BadRequest(Json.toJson(errors)))
            }
          }
        } recoverTotal {
          jsError => {
            ExceptionHelper.log_warning(this.getClass,"JsError[JoinModel]",Some(jsError.errors.map({case (path,errorList) => "  " + path.toString + "\n" + errorList.map(error => "    " + error.message).mkString("\n")}).mkString("\n")),None,Some(request))
            Future(BadRequest(Json.toJson("fail")))
          }
        }
      }
      case _ => Future(Forbidden(Json.toJson("fail")))
    }
  }

}
