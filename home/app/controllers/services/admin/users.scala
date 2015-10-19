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

package controllers.services.admin

import play.api.cache._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import scala.annotation.meta.field
import scala.collection.{JavaConversions}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import java.text.SimpleDateFormat
import java.util.{Calendar,Date}
import javax.inject.Inject
import javax.ws.rs.{QueryParam, PathParam}
import be.objectify.deadbolt.scala.{ActionBuilders,DeadboltActions}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.DBObject
import com.wordnik.swagger.annotations._
import jp.t2v.lab.play2.auth._
import org.apache.log4j.Logger
import org.bson.types._
import com.rodneylai.auth._
import com.rodneylai.models.mongodb._
import com.rodneylai.security._
import com.rodneylai.util._

@ApiModel("UserModel")
case class UserModel( @(ApiModelProperty @field)(position=1,value="friendly_url",required=true)id: String,
                      @(ApiModelProperty @field)(position=2,required=true)email: String,
                      @(ApiModelProperty @field)(position=3,required=true)name: String,
                      @(ApiModelProperty @field)(position=4,required=true)roles: Set[String],
                      @(ApiModelProperty @field)(position=5,required=true)status: String,
                      @(ApiModelProperty @field)(position=6,required=true)added_date: String,
                      @(ApiModelProperty @field)(position=7,required=true)added_ago: String)

@Api(value = "/admin/users", description = "admin users services")
class users @Inject() (deadbolt: DeadboltActions, actionBuilder: ActionBuilders) extends Controller with AuthElement with AuthConfigImpl {

  private val m_log:Logger = Logger.getLogger(this.getClass.getName)

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
  def save(@ApiParam(value = "friendly_url", required = true) @PathParam("friendly_url") friendlyUrl:String) =
    AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("admin"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        if (play.api.Play.isDev(play.api.Play.current)) {
          Thread.sleep(2000)
        }
        request.body match {
          case AnyContentAsJson(json) => {
            json.validate[UserModel] map {
              case user => if (MongoHelper.isActive) {
                val collection:MongoCollection = UserAccount.getCollection
                val query:MongoDBObject = MongoDBObject("FriendlyUrl" -> friendlyUrl.toLowerCase)

                collection.findOne( query ) match {
                  case Some(userAccountBson) => {
                    val userAccount:UserAccount = UserAccountMap.fromBson(userAccountBson)

                    if ((user.email == userAccount.emailAddress) && (user.name == userAccount.name)) {
                      if ((user.status != userAccount.status) || (user.roles != userAccount.roleList)) {
                        collection.update(query,UserAccountMap.toBson(UserAccount(
                          userAccount.id,
                          userAccount.userUuid,
                          userAccount.passwordHash,
                          userAccount.emailAddress,
                          userAccount.emailAddressLowerCase,
                          userAccount.name,
                          userAccount.friendlyUrl,
                          user.roles,
                          user.status,
                          Calendar.getInstance.getTime,
                          userAccount.createDateTimeUTC
                        )))
                      }
                      Ok(Json.toJson("okay"))
                    } else {
                      BadRequest(Json.toJson("fail"))
                    }
                  }
                  case None => NotFound(Json.toJson("fail"))
                }
              } else {
                BadRequest(Json.toJson("fail"))
              }
            } recoverTotal {
              jsError => {
                ExceptionHelper.log_warning(this.getClass,"JsError[UserModel]",Some(jsError.errors.map({case (path,errorList) => "  " + path.toString + "\n" + errorList.map(error => "    " + error.message).mkString("\n")}).mkString("\n")),None,Some(request))
                BadRequest(Json.toJson("fail"))
              }
            }
          }
          case _ => BadRequest(Json.toJson("fail"))
        }
      }
    }.apply(request)
  }

  @ApiOperation(value = "get", notes = "returns value", nickname="get", response = classOf[UserModel], httpMethod = "GET")
  def get(@ApiParam(value = "filter", required = false) @QueryParam("filter") filter:String,
          @ApiParam(value = "skip", required = false) @QueryParam("skip") skip:Int) =
    AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("admin"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        val formatter:SimpleDateFormat = new SimpleDateFormat("MM/dd/yyyy kk:mm:ss:SSSS")

        if (play.api.Play.isDev(play.api.Play.current)) {
          Thread.sleep(2000)
        }
        if (MongoHelper.isActive) {
          Ok(Json.toJson(UserAccount.getCollection.find().sort(MongoDBObject("EmailAddressLowerCase" -> 1)).skip(skip).limit(10).toSeq.map(
            userAccountBson => {
              val userAccount:UserAccount = UserAccountMap.fromBson(userAccountBson)

              UserModel(userAccount.friendlyUrl,userAccount.emailAddress,userAccount.name,userAccount.roleList,userAccount.status,formatter.format(userAccount.createDateTimeUTC),UtilityHelper.getDateAgoString(userAccount.createDateTimeUTC))
            }
          )))
        } else {
          UserAccount.getTestAccounts match {
            case Some(testAccountList) => Ok(Json.toJson(testAccountList.map(userAccount => UserModel(userAccount.friendlyUrl,userAccount.emailAddress,userAccount.name,userAccount.roleList,userAccount.status,formatter.format(userAccount.createDateTimeUTC),UtilityHelper.getDateAgoString(userAccount.createDateTimeUTC)))))
            case None => BadRequest(Json.toJson("fail"))
          }
        }
      }
    }.apply(request)
  }


}


