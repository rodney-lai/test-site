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

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with GuiceOneAppPerTest {

  "Application" should {

    "send 404 on a bad request" in {
      val boum = route(app,FakeRequest(GET, "/boum")).get

      status(boum)(akka.util.Timeout(200000,java.util.concurrent.TimeUnit.MILLISECONDS)) mustBe SEE_OTHER
      val newLocationUrl = headers(boum).getOrElse("Location","/")
      status(cookies(boum).get("PLAY_FLASH") match {
        case Some(cookie) => route(app,FakeRequest(GET, newLocationUrl).withCookies(cookie)).get
        case None => route(app,FakeRequest(GET, newLocationUrl)).get
      })(akka.util.Timeout(200000,java.util.concurrent.TimeUnit.MILLISECONDS)) mustBe NOT_FOUND
    }

    "render the index page" in {
      val home = route(app,FakeRequest(GET, "/")).get

      status(home)(akka.util.Timeout(200000,java.util.concurrent.TimeUnit.MILLISECONDS)) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Rodney's Test Server")
    }
  }
}
