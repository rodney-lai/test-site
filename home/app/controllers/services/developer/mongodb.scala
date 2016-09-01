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
import scala.collection.{JavaConversions}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.concurrent.duration._
import java.text.SimpleDateFormat
import javax.inject.Inject
import javax.ws.rs.{QueryParam, PathParam}
import be.objectify.deadbolt.scala.{ActionBuilders,DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonArray,BsonBinary,BsonDateTime,BsonInt64,BsonObjectId,BsonString}
import com.wordnik.swagger.annotations._
import jp.t2v.lab.play2.auth._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._
import com.rodneylai.database._
import com.rodneylai.models.mongodb._
import com.rodneylai.security._
import com.rodneylai.stackc.DevModeDelay
import com.rodneylai.util._

@ApiModel("MongoDBResultsModel")
case class MongoDBResultsModel( @ApiModelProperty(position=1,required=true)date: String,
                                @ApiModelProperty(position=2,required=true)json: String)

@Api(value = "/developer-mongodb", description = "developer mongodb services")
class mongodb @Inject() (environment:play.api.Environment,deadbolt:DeadboltActions,actionBuilder:ActionBuilders,mongoHelper:MongoHelper,override val accountDao:AccountDao) extends Controller with AuthElement with AuthConfigImpl with DevModeDelay {

  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  implicit val mongodbResultsModelFormat = Json.format[MongoDBResultsModel]

  override def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.toJson("kick")))
  }

  private def mapMongoToJson(bsonValueOption:Option[Any]):JsValue = {
    bsonValueOption match {
      case Some(bsonValue) => bsonValue match {
        case bsonObjectId:BsonObjectId => JsString(bsonObjectId.getValue.toString)
        case bsonArray:BsonArray => JsArray(JavaConversions.asScalaBuffer(bsonArray.getValues).toSeq.map(_ match { case bsonString:BsonString => JsString(bsonString.getValue) }))
        case bsonDateTime:BsonDateTime => {
          val formatter:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

          JsString(formatter.format(bsonDateTime.getValue))
        }
        case bsonString:BsonString => JsString(bsonString.getValue)
        case bsonBinary:BsonBinary if (bsonBinary.getType == org.bson.BsonBinarySubType.UUID_STANDARD.getValue) => JsString(MongoHelper.fromStandardBinaryUUID(bsonBinary.getData).toString)
        case bsonInt64:BsonInt64 => JsNumber(bsonInt64.getValue)
        case _ => JsString(bsonValue.toString)
      }
      case None => JsString("[ NONE ]")
    }
  }

  @ApiOperation(value = "get", notes = "returns value", nickname="get", response = classOf[MongoDBResultsModel], httpMethod = "GET")
  def get(@ApiParam(value = "collection_name", required = true) @PathParam("collection_name") collectionName:String,
          @ApiParam(value = "skip", required = false) @QueryParam("skip") skip:Int) =
    AsyncStack(AuthorityKey -> Role.Administrator,EnvironmentKey -> environment) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action.async {
        for {
          documents <- mongoHelper.getCollection(collectionName).find().skip(skip).limit(10).toFuture
        } yield {
          Ok(Json.toJson(documents.toSeq.map({ x =>
            MongoDBResultsModel(
              x.get[BsonObjectId]("_id").get.getValue.getDate.toString,
              JsObject(x.keySet.toSeq.map({ key => key -> mapMongoToJson(x.get(key)) })).toString
            )
          })))
        }
      }
    }.apply(request)
  }

}
