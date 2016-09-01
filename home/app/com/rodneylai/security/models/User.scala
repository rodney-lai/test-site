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

package com.rodneylai.security.models

import play.libs.Scala
import be.objectify.deadbolt.core.models.Subject
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.models.mongodb._
import com.rodneylai.util._

class User(userName:String,roleList:Set[String]) extends Subject
{
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  def getRoles: java.util.List[SecurityRole] = {
    Scala.asJava(roleList.toSeq.map(role => new SecurityRole(role)))
  }

  def getPermissions: java.util.List[UserPermission] = {
    Scala.asJava(List())
  }

  def getIdentifier: String = userName
}
