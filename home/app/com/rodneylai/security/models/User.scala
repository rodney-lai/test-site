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

package com.rodneylai.security.models

import play.libs.Scala
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.DBObject
import be.objectify.deadbolt.core.models.Subject
import org.bson.types._
import com.rodneylai.models.mongodb._
import com.rodneylai.util._

class User(val userName: String) extends Subject
{
  def getRoles: java.util.List[SecurityRole] = {
    if (MongoHelper.isActive) {
      UserAccount.getCollection.findOne( MongoDBObject("EmailAddressLowerCase" -> userName.toLowerCase), MongoDBObject("RoleList" -> 1) ) match {
        case Some(userAccountBson) => Scala.asJava(userAccountBson.getAsOrElse[List[String]]("RoleList",List[String]()).map(x => new SecurityRole(x)))
        case None => Scala.asJava(List())
      }
    } else {
      UserAccount.findByEmailAddress(userName) match {
        case Some(userAccount) => Scala.asJava(userAccount.roleList.toSeq.map(role => new SecurityRole(role)))
        case None => Scala.asJava(List())
      }
    }
  }

  def getPermissions: java.util.List[UserPermission] = {
    Scala.asJava(List())
  }

  def getIdentifier: String = userName
}