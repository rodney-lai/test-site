/**
 *
 * Copyright (c) 2015-2017 Rodney S.K. Lai
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

package controllers

import play.api.mvc._
import javax.inject._
import io.swagger.annotations._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.util._

@Singleton
@Api(value = "test", description = "test services")
class TestController @Inject() (
  configuration: play.api.Configuration,
  infoHelper:InfoHelper,
  cc: ControllerComponents
) extends AbstractController(cc) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  m_log.debug("init")

  @ApiOperation(value = "throw exception", nickname = "throw_exception", httpMethod = "GET")
  def throw_exception = Action {
    val x:Integer = 10
    val y:Integer = 0
    val z:Integer = x/y
    Ok(z.toString)
  }

  @ApiOperation(value = "test value", nickname = "test_val", httpMethod = "GET")
  def test_val(x:Integer) = Action {
    Ok
  }

  @ApiOperation(value = "build date", nickname = "build_date", httpMethod = "GET")
  def build_date = Action {
    infoHelper.getBuildDate match {
      case Some(buildDate) => Ok(buildDate)
      case None => NotFound
    }
  }
}
