/**
 *
 * Copyright (c) 2015-2016 Rodney S.K. Lai
 * https://github.com/rodney-lai
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
import play.api.Mode
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure, Success, Try}
import javax.inject.Inject
import com.wordnik.swagger.annotations._
import jp.t2v.lab.play2.auth._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._
import com.rodneylai.database._
import com.rodneylai.models.mongodb._
import com.rodneylai.stackc.DevModeDelay
import com.rodneylai.util._

@ApiModel("LoginModel")
case class LoginModel(@ApiModelProperty(position=1,required=true)login: String,
                      @ApiModelProperty(position=2,required=true)password: String)

@ApiModel("JoinModel")
case class JoinModel( @ApiModelProperty(position=1,required=true)full_name: String,
                      @ApiModelProperty(position=2,required=true)email: String,
                      @ApiModelProperty(position=3,required=true)user_name: String,
                      @ApiModelProperty(position=4,required=true)password: String)

@Api(value = "/auth", description = "authentication services")
class auth @Inject() (environment:play.api.Environment,authHelper:AuthHelper,mongoHelper:MongoHelper,userAccountDao:UserAccountDao,override val accountDao:AccountDao) extends Controller with LoginLogout with AuthConfigImpl with DevModeDelay {

  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)
  private val m_validateEmailAddressRegEx:String = """[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]+"""
  private val m_validateFriendlyUrlRegEx:String = """^(?=.{3,}$)(?!-)(?!.*--)[a-z0-9-]*(?<!-)"""
  private val m_validatePasswordRegEx:String = """^(?=.{8,}$)(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).*"""

  implicit val loginModelFormat = Json.format[LoginModel]
  implicit val joinModelFormat = Json.format[JoinModel]

  private def findUser(login:String):Future[Option[UserAccount]] = {
    if (login.contains("@")) {
      userAccountDao.findByEmailAddress(login)
    } else {
      userAccountDao.findByFriendlyUrl(login)
    }
  }

  private def validateLogin(login:String,password:String):Future[Option[(java.util.UUID,Set[String])]] = {
    for {
      userAccountOption <- findUser(login)
    } yield {
      userAccountOption match {
        case Some(userAccount) if (authHelper.validatePassword(password,userAccount.passwordHash)) => Some((userAccount.userUuid,userAccount.roleList))
        case _ => None
      }
    }
  }

  def loginUserSucceeded: Future[Result] = {
    Future.successful(Ok(Json.toJson(controllers.routes.Application.index.url)))
  }

  def loginDeveloperSucceeded: Future[Result] = {
    Future.successful(Ok(Json.toJson(controllers.routes.Developer.index.url)))
  }

  def loginAdminSucceeded: Future[Result] = {
    Future.successful(Ok(Json.toJson(controllers.routes.Admin.index.url)))
  }

  @ApiOperation(value = "login", notes = "returns login redirect url", nickname="login", response = classOf[String], httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "login credentials", required = true, dataType = "controllers.services.LoginModel", paramType = "body")))
  def login = Action.async(parse.json[LoginModel]) { implicit request =>
    val loginModel:LoginModel = request.body

    for {
      loginResultOption <- validateLogin(loginModel.login,loginModel.password)
      result <- loginResultOption match {
        case Some((userUuid,roleList)) => {
          if (roleList.contains("developer")) {
            gotoLoginSucceeded(userUuid, loginDeveloperSucceeded)
          } else if (roleList.contains("admin")) {
            gotoLoginSucceeded(userUuid, loginAdminSucceeded)
          } else {
            gotoLoginSucceeded(userUuid, loginUserSucceeded)
          }
        }
        case None => Future.successful(Forbidden(Json.toJson("fail")))
      }
    } yield result
  }

  @ApiOperation(value = "join", notes = "returns join status", nickname="join", response = classOf[String], httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "join information", required = true, dataType = "controllers.services.JoinModel", paramType = "body")))
  def join = AsyncStack(parse.json[JoinModel],EnvironmentKey -> environment) { implicit request =>
    val joinModel:JoinModel = request.body

    for {
      errorsOption:Seq[Option[String]] <- Future.sequence(Seq(
        if (joinModel.full_name.trim.isEmpty) {
          Future.successful(Some("Full name is required."))
        } else {
          Future.successful(None)
        },
        if (joinModel.email.trim.isEmpty) {
          Future.successful(Some("Email address is required."))
        } else if (!joinModel.email.trim.matches(m_validateEmailAddressRegEx)) {
          Future.successful(Some("Email address is not valid."))
        } else {
          for {
            userAccountOption <- userAccountDao.findByEmailAddress(joinModel.email.trim)
          } yield {
            userAccountOption match {
              case Some(userAccount) => Some("Email address is already being used by another account.")
              case None => None
            }
          }
        },
        if (joinModel.user_name.trim.isEmpty) {
          Future.successful(Some("User name is required."))
        } else if (!joinModel.user_name.trim.matches(m_validateFriendlyUrlRegEx)) {
          Future.successful(Some("User name is not valid.  User name MUST be at least three characters and can consist of lower case letters, digits, and dashes ONLY.  User name cannot start or end with a dash and there cannot be two consecutive dashes."))
        } else {
          for {
            userAccountOption <- userAccountDao.findByFriendlyUrl(joinModel.user_name.trim)
          } yield {
            userAccountOption match {
              case Some(userAccount) => Some("User Name is already being used by another account.")
              case None => None
            }
          }
        },
        if (joinModel.password.trim.isEmpty) {
          Future.successful(Some("Password is required."))
        } else if (!joinModel.password.trim.matches(m_validatePasswordRegEx)) {
          Future.successful(Some("Password is not valid.  Password MUST be at least eight characters and contain at least one uppercase letter, one lowercase letter, and one digit."))
        } else {
          Future.successful(None)
        }
      ))
      errors:Seq[String] = errorsOption.flatten
      result <- if (errors.isEmpty) {
        if (mongoHelper.isActive) {
          val now:java.util.Date = java.util.Calendar.getInstance.getTime
          val userAccount:UserAccount = UserAccount(java.util.UUID.randomUUID,
                                                    authHelper.hashPassword(joinModel.password.trim),
                                                    joinModel.email.trim,
                                                    joinModel.email.trim.toLowerCase,
                                                    joinModel.full_name.trim,
                                                    joinModel.user_name.trim,
                                                    Set[String](),
                                                    "unconfirmed",
                                                    now,
                                                    now)

          {
            for {
              collection <- userAccountDao.collectionFuture
              _ <- collection.insertOne(userAccountDao.toBson(userAccount)).toFuture
            } yield Ok(Json.toJson("okay"))
          } recoverWith { case ex =>
            m_log.error("failed to add new user",ex)
            Future.successful(InternalServerError(Json.toJson("fail")))
          }
        } else {
          Future.successful(BadRequest(Json.toJson(Seq[String]("Datastore NOT configured.  User not created.  Try again later."))))
        }
      } else {
        Future.successful(BadRequest(Json.toJson(errors)))
      }
    } yield result
  }

}
