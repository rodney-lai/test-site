/**
 *
 * Copyright (c) 2015-2016 Rodney S.K. Lai
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

package controllers

import play.api._
import play.api.mvc._
import scala.collection.{JavaConversions}
import scala.collection.mutable.{Buffer}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.{Regex}
import javax.inject.Inject
import be.objectify.deadbolt.scala.{ActionBuilders,DeadboltActions}
import jp.t2v.lab.play2.auth._
import net.spy.memcached.{AddrUtil,ConnectionFactoryBuilder,MemcachedClient}
import net.spy.memcached.ConnectionFactoryBuilder.{Protocol}
import net.spy.memcached.auth.{AuthDescriptor,PlainCallbackHandler}
import com.rodneylai.auth._
import com.rodneylai.security._
import com.rodneylai.stackc._
import com.rodneylai.util._

class Developer @Inject() (configuration: play.api.Configuration, deadbolt: DeadboltActions, actionBuilder: ActionBuilders)(implicit environment: play.api.Environment) extends Controller with TrackingPageViewAuth with AuthElement with AuthConfigImpl with RequireSSL {

  def index = server

  def server = AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        Ok(views.html.developer.server(loggedIn,InfoHelper.getMachineInfo ++ InfoHelper.getApplicationInfo))
      }
    }.apply(request)
  }

  def memcached = AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        val moduleList:Buffer[String] = configuration.getList("play.modules.enabled") match {
                                          case Some(enabledList) => {
                                            configuration.getList("play.modules.disabled") match {
                                              case Some(disabledList) => JavaConversions.asScalaBuffer(enabledList.unwrapped).map(_.toString) --= JavaConversions.asScalaBuffer(disabledList.unwrapped).map(_.toString)
                                              case None => JavaConversions.asScalaBuffer(enabledList.unwrapped).map(_.toString)
                                            }
                                          }
                                          case None => Buffer[String]()
                                        }

        configuration.getString("memcached.host") match {
          case Some(host) if (moduleList.contains("com.github.mumoshu.play2.memcached.MemcachedModule")) => {
            val memcachedClient:MemcachedClient = (configuration.getString("memcached.user"),configuration.getString("memcached.password")) match {
                                                    case (Some(user),Some(password)) => {
                                                      val authDescriptor:AuthDescriptor = new AuthDescriptor(Array("PLAIN"), new PlainCallbackHandler(user, password))

                                                      new MemcachedClient(
                                                        new ConnectionFactoryBuilder().setProtocol(Protocol.BINARY)
                                                          .setAuthDescriptor(authDescriptor)
                                                          .build(),
                                                        AddrUtil.getAddresses(host)
                                                      )

                                                    }
                                                    case _ => new MemcachedClient(AddrUtil.getAddresses(host))
                                                  }
            val stats:Map[String,Seq[(String,String)]] = JavaConversions.mapAsScalaMap(memcachedClient.getStats).map({case (host,stats) => host.toString -> JavaConversions.mapAsScalaMap(stats).toSeq.sortBy(_._1) }).toMap

            Ok(views.html.developer.memcached(loggedIn,Some(stats)))
          }
          case _ => {
            val cacheModuleList:Buffer[String] = moduleList.filter(_.toLowerCase.contains("cache"))

            if (cacheModuleList.size == 0) {
              Ok(views.html.developer.memcached(loggedIn,None,Some("Memcached does NOT appear to be configured, possibly using internal cache.")))
            } else {
              Ok(views.html.developer.memcached(loggedIn,None,Some("Memcached does NOT appear to be configured, possibly using [" + cacheModuleList.mkString(",") + "]")))
            }
          }
        }
      }
    }.apply(request)
  }

  def mongodb = AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action.async {
        for {
          statsOption <- MongoHelper.getDatabaseStats
          collectionListOption <- MongoHelper.getCollectionList
        } yield Ok(views.html.developer.mongodb(loggedIn,statsOption,collectionListOption))
      }
    }.apply(request)
  }

  def mongodb_collection(collectionName:String) = AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action.async {
        for {
          statsOption <- MongoHelper.getCollectionStats(collectionName)
          collectionListOption <- MongoHelper.getCollectionList
        } yield Ok(views.html.developer.mongodb_collection(loggedIn,collectionName,statsOption,collectionListOption))
      }
    }.apply(request)
  }

  def api = AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        Ok(views.html.developer.api_docs(loggedIn))
      }
    }.apply(request)
  }

}
