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

package controllers.services.developer

import play.api.cache._
import play.api.libs.json._
import play.api.Mode
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.concurrent.duration._
import javax.inject.Inject
import javax.ws.rs.{QueryParam, PathParam}
import be.objectify.deadbolt.scala.{ActionBuilders,DeadboltActions}
import com.wordnik.swagger.annotations._
import jp.t2v.lab.play2.auth._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._
import com.rodneylai.security._
import com.rodneylai.stackc.DevModeDelay

@ApiModel("MemcachedValueModel")
case class MemcachedValueModel(@ApiModelProperty(position=1,required=true)value: String)

@ApiModel("MemcachedResultsModel")
case class MemcachedResultsModel( @ApiModelProperty(position=1,required=true)id: String,
                                  @ApiModelProperty(position=2,required=true)value: String)

@Api(value = "/developer-memcached", description = "developer memcached services")
class memcached @Inject() (environment: play.api.Environment, deadbolt: DeadboltActions, actionBuilder: ActionBuilders,override val accountDao:AccountDao)(implicit app: play.api.Application) extends Controller with AuthElement with AuthConfigImpl with DevModeDelay {

  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)
  private val m_cache = play.api.cache.Cache

  implicit val memcachedValueModelFormat = Json.format[MemcachedValueModel]
  implicit val memcachedResultsModelFormat = Json.format[MemcachedResultsModel]

  override def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  @ApiOperation(value = "get", notes = "returns value", nickname="get", response = classOf[String], httpMethod = "GET")
  def get(@ApiParam(value = "key", required = true) @PathParam("key") key:String) =
    AsyncStack(AuthorityKey -> Role.Administrator,EnvironmentKey -> environment) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        m_cache.get(key) match {
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
    AsyncStack(parse.json[MemcachedValueModel],AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action(parse.json[MemcachedValueModel]) { _ =>
        val memcachedValueModel:MemcachedValueModel = request.body

        if (key.trim.isEmpty) {
          BadRequest(Json.toJson("fail"))
        } else {
          m_cache.set(key,memcachedValueModel.value)
          Ok(Json.toJson("okay"))
        }
      }
    }.apply(request)
  }

  @ApiOperation(value = "clear", notes = "returns status", nickname="clear", response = classOf[String], httpMethod = "DELETE")
  def clear(@ApiParam(value = "key", required = true) @PathParam("key") key:String) =
    AsyncStack(AuthorityKey -> Role.Administrator,EnvironmentKey -> environment) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        m_cache.remove(key)
        Ok(Json.toJson("okay"))
      }
    }.apply(request)
  }


}
