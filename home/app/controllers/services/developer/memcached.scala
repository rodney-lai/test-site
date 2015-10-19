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

package controllers.services.developer

import play.api.cache._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import scala.annotation.meta.field
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.concurrent.duration._
import javax.inject.Inject
import javax.ws.rs.{QueryParam, PathParam}
import be.objectify.deadbolt.scala.{ActionBuilders,DeadboltActions}
import com.wordnik.swagger.annotations._
import jp.t2v.lab.play2.auth._
import org.apache.log4j.Logger
import com.rodneylai.auth._
import com.rodneylai.security._

@ApiModel("MemcachedValueModel")
case class MemcachedValueModel(@(ApiModelProperty @field)(position=1,required=true)value: String)

@ApiModel("MemcachedResultsModel")
case class MemcachedResultsModel( @(ApiModelProperty @field)(position=1,required=true)id: String,
                                  @(ApiModelProperty @field)(position=2,required=true)value: String)

@Api(value = "/developer/memcached", description = "developer memcached services")
class memcached @Inject() (deadbolt: DeadboltActions, actionBuilder: ActionBuilders) extends Controller with AuthElement with AuthConfigImpl {

  private val m_log:Logger = Logger.getLogger(this.getClass.getName)

  implicit val memcachedResultsModelFormat = Json.format[MemcachedResultsModel]

  override def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  @ApiOperation(value = "get", notes = "returns value", nickname="get", response = classOf[String], httpMethod = "GET")
  def get(@ApiParam(value = "key", required = true) @PathParam("key") key:String) =
    AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        if (play.api.Play.isDev(play.api.Play.current)) {
          Thread.sleep(2000)
        }
        Cache.get(key) match {
          case Some(value) => Ok(Json.toJson(MemcachedResultsModel(key,value.toString)))
          case None => NotFound(Json.toJson(MemcachedResultsModel(key,"[not found]")))
        }
      }
    }.apply(request)
  }

  @ApiOperation(value = "set", notes = "returns status", nickname="set", response = classOf[String], httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "key/value", required = true, dataType = "controllers.services.developer.MemcachedValueModel", paramType = "body")))
  def set(@ApiParam(value = "key", required = true) @PathParam("key") key:String) =
    AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        request.body match {
          case AnyContentAsJson(json) => {
            ((json \ "value").asOpt[String]) match {
              case Some(value) if (!key.isEmpty) => {
                Cache.set(key,value)
                Ok(Json.toJson("okay"))
              }
              case _ => Ok(Json.toJson("fail"))
            }
          }
          case _ => Ok(Json.toJson("fail"))
        }
      }
    }.apply(request)
  }

  @ApiOperation(value = "clear", notes = "returns status", nickname="clear", response = classOf[String], httpMethod = "DELETE")
  def clear(@ApiParam(value = "key", required = true) @PathParam("key") key:String) =
    AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        if (play.api.Play.isDev(play.api.Play.current)) {
          Thread.sleep(2000)
        }
        Cache.remove(key)
        Ok(Json.toJson("okay"))
      }
    }.apply(request)
  }


}


