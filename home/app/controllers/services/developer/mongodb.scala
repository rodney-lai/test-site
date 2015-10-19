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
import scala.collection.{JavaConversions}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.concurrent.duration._
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


@ApiModel("MongoDBResultsModel")
case class MongoDBResultsModel( @(ApiModelProperty @field)(position=1,required=true)date: String,
                                @(ApiModelProperty @field)(position=2,required=true)json: String)

@Api(value = "/developer/mongodb", description = "developer mongodb services")
class mongodb @Inject() (deadbolt: DeadboltActions, actionBuilder: ActionBuilders) extends Controller with AuthElement with AuthConfigImpl {

  private val m_log:Logger = Logger.getLogger(this.getClass.getName)

  implicit val mongodbResultsModelFormat = Json.format[MongoDBResultsModel]

  override def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  private def mapMongoToJson(t:Any):JsValue = {
    t match {
      case value:com.mongodb.BasicDBList => JsArray(JavaConversions.asScalaBuffer(value).map({x => mapMongoToJson(x)}).toSeq)
      case _ => {
        JsString( t match {
                    case value:Binary => MongoHelper.fromStandardBinaryUUID(value).toString
                    case value:Any => value.toString
                  }
        )
      }
    }
  }

  @ApiOperation(value = "get", notes = "returns value", nickname="get", response = classOf[MongoDBResultsModel], httpMethod = "GET")
  def get(@ApiParam(value = "collection_name", required = true) @PathParam("collection_name") collectionName:String,
          @ApiParam(value = "skip", required = false) @QueryParam("skip") skip:Int) =
    AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        if (play.api.Play.isDev(play.api.Play.current)) {
          Thread.sleep(2000)
        }
        MongoHelper.getCollection(collectionName) match {
          case Some(collection) => Ok(Json.toJson(collection.find().skip(skip).limit(10).toSeq.map({ x =>
            MongoDBResultsModel(
              x.as[ObjectId]("_id").getDate.toString,
              JsObject(JavaConversions.asScalaSet(x.keySet).toSeq.map({ key => key -> mapMongoToJson(x.get(key))})).toString
            )
          })))
          case None => NotFound(Json.toJson(Seq[String]()))
        }
      }
    }.apply(request)
  }


}


