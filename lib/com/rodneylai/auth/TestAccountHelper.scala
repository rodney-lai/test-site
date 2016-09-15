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

package com.rodneylai.auth

import play.api.mvc._
import scala.concurrent.{ExecutionContext,Future}
import java.util.{Calendar,Date}
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.mindrot.jbcrypt._
import com.rodneylai.util._

@Singleton
class TestAccountHelper @Inject() (configuration:play.api.Configuration) {
  case class TestAccount(email:String,name:String,friendlyUrl:String,roleList:Set[String])

  val testAccounts:Seq[TestAccount] = Seq(TestAccount("normal_user@rodneylai.com","Test User","test-user",Set[String]()),
                                          TestAccount("admin_user@rodneylai.com","Test Admin","test-admin",Set[String]("admin")),
                                          TestAccount("developer_user@rodneylai.com","Test Developer","test-developer",Set[String]("admin","developer"))
                                        )

  private def testPassword:String =
    configuration.getString("test.password") match {
      case Some(testPassword) if (testPassword != "changeme") => testPassword
      case Some(testPassword) if (testPassword == "changeme") => throw new Exception("Change default value of test.password config key.")
      case None => throw new Exception("Missing test.password config key.")
    }

  def testPasswordHash:String = BCrypt.hashpw(testPassword, BCrypt.gensalt(14))

}

class TestAccountHelperModule extends AbstractModule {
  def configure() = {
    bind(classOf[TestAccountHelper]).asEagerSingleton
  }
}
