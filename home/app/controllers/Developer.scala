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

package controllers

import play.api._
import play.api.mvc._
import scala.collection.{JavaConversions}
import scala.collection.mutable.{Buffer}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.{Regex}
import java.net.{InetSocketAddress,SocketAddress}
import javax.inject.Inject
import be.objectify.deadbolt.scala.{ActionBuilders,DeadboltActions}
import jp.t2v.lab.play2.auth._
import net.spy.memcached.{MemcachedClient}
import com.rodneylai.auth._
import com.rodneylai.security._
import com.rodneylai.stackc._
import com.rodneylai.util._

class Developer @Inject() (deadbolt: DeadboltActions, actionBuilder: ActionBuilders) extends Controller with TrackingPageViewAuth with AuthElement with AuthConfigImpl with RequireSSL {

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
        val memcachedHostRegex:Regex = """([-0-9a-zA-Z.+_]+)\:([0-9]+)""".r
        val moduleList:Buffer[String] = play.api.Play.current.configuration.getList("play.modules.enabled") match {
                                          case Some(enabledList) => {
                                            play.api.Play.current.configuration.getList("play.modules.disabled") match {
                                              case Some(disabledList) => JavaConversions.asScalaBuffer(enabledList.unwrapped).map(_.toString) --= JavaConversions.asScalaBuffer(disabledList.unwrapped).map(_.toString)
                                              case None => JavaConversions.asScalaBuffer(enabledList.unwrapped).map(_.toString)
                                            }
                                          }
                                          case None => Buffer[String]()
                                        }

        play.api.Play.current.configuration.getString("memcached.host") match {
          case Some(memcachedHostRegex(host,port)) if (moduleList.contains("com.github.mumoshu.play2.memcached.MemcachedModule")) => {
            val memcachedClient:MemcachedClient = new MemcachedClient(new InetSocketAddress(host,port.toInt))
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
      Action {
        Ok(views.html.developer.mongodb(loggedIn))
      }
    }.apply(request)
  }

  def mongodb_collection(collectionName:String) = AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
    deadbolt.Restrict(Array("developer"), new DefaultDeadboltHandler(Some(loggedIn))) {
      Action {
        Ok(views.html.developer.mongodb_collection(loggedIn,collectionName))
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
