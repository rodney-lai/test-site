/**
 *
 * Copyright (c) 2015-2017 Rodney S.K. Lai
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

package controllers.services.admin

import play.api.cache._
import play.api.libs.json._
import play.api.Mode
import play.api.mvc._
import scala.collection.{JavaConversions}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import java.text.SimpleDateFormat
import java.util.{Calendar,Date}
import javax.inject.Inject
import be.objectify.deadbolt.scala.{ActionBuilders,DeadboltActions}
import io.swagger.annotations._
import jp.t2v.lab.play2.auth._
import org.mongodb.scala._
import org.mongodb.scala.model.Sorts._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._
import com.rodneylai.database._
import com.rodneylai.models.mongodb._
import com.rodneylai.security._
import com.rodneylai.stackc.DevModeDelay
import com.rodneylai.util._

@ApiModel("UserModel")
case class UserModel( @ApiModelProperty(position=1,value="friendly_url",required=true)id: String,
                      @ApiModelProperty(position=2,required=true)email: String,
                      @ApiModelProperty(position=3,required=true)name: String,
                      @ApiModelProperty(position=4,required=true)roles: Set[String],
                      @ApiModelProperty(position=5,required=true)status: String,
                      @ApiModelProperty(position=6,required=true)added_date: String,
                      @ApiModelProperty(position=7,required=true)added_ago: String)

@Api(value = "/admin-users", description = "admin users services")
class users @Inject() (override val environment:play.api.Environment,override val configuration:play.api.Configuration,deadbolt:DeadboltActions,actionBuilder:ActionBuilders,mongoHelper:MongoHelper,userAccountDao:UserAccountDao,override val accountDao:AccountDao) extends Controller with AuthElement with AuthConfigImpl with DevModeDelay {

  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  implicit val userModelFormat = Json.format[UserModel]

  override def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  @ApiOperation(value = "save", notes = "returns status", nickname="save", response = classOf[String], httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "user", required = true, dataType = "controllers.services.admin.UserModel", paramType = "body")))
  def save(@ApiParam(value = "friendly_url", required = true) friendlyUrl:String) =
    AsyncStack(parse.json[UserModel],AuthorityKey -> Role.Administrator,EnvironmentKey -> environment) { implicit request =>
    deadbolt.Restrict(List(Array("admin")), new DefaultDeadboltHandler(Some(loggedIn)))(parse.json[UserModel]) { authRequest =>
      val userModel:UserModel = request.body

      if (mongoHelper.isActive) {
        for {
          userAccountOption <- userAccountDao.findByFriendlyUrl(friendlyUrl)
          result <- userAccountOption match {
            case Some(userAccount) => {
              if ((userModel.email == userAccount.emailAddress) && (userModel.name == userAccount.name)) {
                if ((userModel.status != userAccount.status) || (userModel.roles != userAccount.roleList)) {
                  {
                    for {
                      collection <- userAccountDao.collectionFuture
                      replaceResult <- collection.replaceOne(
                        Document("FriendlyUrl" -> userAccount.friendlyUrl),
                        userAccountDao.toBson(userAccount.copy(status = userModel.status,roleList = userModel.roles,updateDate = Calendar.getInstance.getTime))
                      ).toFuture
                    } yield Ok(Json.toJson("okay"))
                  } recoverWith { case ex =>
                    m_log.error("failed to update user",ex)
                    Future.successful(InternalServerError(Json.toJson("fail")))
                  }
                } else {
                  Future.successful(Ok(Json.toJson("okay")))
                }
              } else {
                Future.successful(BadRequest(Json.toJson("fail")))
              }
            }
            case None => Future.successful(NotFound(Json.toJson("fail")))
          }
        } yield result
      } else {
        Future.successful(BadRequest(Json.toJson("fail")))
      }
    }.apply(request)
  }

  @ApiOperation(value = "get", notes = "returns value", nickname="get", response = classOf[UserModel], httpMethod = "GET")
  def get(@ApiParam(value = "filter", required = false) filter:String,
          @ApiParam(value = "skip", required = false) skip:Int) =
    AsyncStack(AuthorityKey -> Role.Administrator,EnvironmentKey -> environment) { implicit request =>
    deadbolt.Restrict(List(Array("admin")), new DefaultDeadboltHandler(Some(loggedIn)))() { authRequest =>
      val formatter:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

      if (mongoHelper.isActive) {
        for {
          collection <- userAccountDao.collectionFuture
          userAccountsBson <- collection.find().sort(ascending("EmailAddressLowerCase")).skip(skip).limit(10).toFuture
        } yield {
          Ok(Json.toJson(userAccountsBson.flatMap(
            userAccountBson => userAccountDao.fromBson(userAccountBson).map { userAccount =>
              UserModel(userAccount.friendlyUrl,userAccount.emailAddress,userAccount.name,userAccount.roleList,userAccount.status,formatter.format(userAccount.createDate),UtilityHelper.getDateAgoString(userAccount.createDate))
            }
          )))
        }
      } else {
        userAccountDao.getTestAccounts match {
          case Some(testAccountList) => Future.successful(Ok(Json.toJson(testAccountList.map(userAccount => UserModel(userAccount.friendlyUrl,userAccount.emailAddress,userAccount.name,userAccount.roleList,userAccount.status,formatter.format(userAccount.createDate),UtilityHelper.getDateAgoString(userAccount.createDate))))))
          case None => Future.successful(BadRequest(Json.toJson("fail")))
        }
      }
    }.apply(request)
  }


}
