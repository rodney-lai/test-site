/**
 *
 * Copyright (c) 2020 Rodney S.K. Lai
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

package com.rodneylai.services

import net.spy.memcached.MemcachedClient
import net.spy.memcached.internal.OperationFuture
import org.mockito.ArgumentMatchers._
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.wordspec._
import org.scalatest.matchers.should.Matchers
import scala.concurrent.Future
import zio.{Runtime, Task}

class CacheServiceTests extends AsyncWordSpec
  with Matchers
  with IdiomaticMockito
  with ArgumentMatchersSugar {

  "CacheService" can {
    val key = "theKey"

    "get" should {
      val memcachedValue = "theMemcachedValue"
      val cacheValue = "theCacheValue"

      def TestFixture(
        memCachedClient: MemcachedClient = mock[MemcachedClient]
      )(test: (CacheServiceImpl,MemcachedClient) => Assertion): Future[Assertion] = {
        val memCachedService = mock[MemCachedService]
        val cacheService = spy(new CacheServiceImpl(memCachedService))

        when(
          memCachedService.client
        ).thenReturn(memCachedClient)
        Option(memCachedClient).map { memCachedClient =>
          when(
            memCachedClient.get(anyString)
          ).thenReturn(null)
          when(
            memCachedClient.get(key)
          ).thenReturn(memcachedValue)
        }
        cacheService.cache.addOne((key -> cacheValue))
        Future.successful(test(cacheService,memCachedClient))
      }

      "find value in memcached if memcached available" in {
        TestFixture() { (cacheService,memCachedClient) =>
          val result = cacheService.get(key)

          verify(memCachedClient, times(1)).get(anyString)
          result shouldBe Some(memcachedValue)
        }
      }

      "find value in internal cache map if memcached not available" in {
        TestFixture(null) { (cacheService,memCachedClient) =>
          val result = cacheService.get(key)

          result shouldBe Some(cacheValue)
        }
      }
    }

    "set" should {
      val value = "theValue"

      def TestFixture(
        memCachedClient: MemcachedClient = mock[MemcachedClient]
      )(test: (CacheServiceImpl,MemcachedClient) => Task[Assertion]): Future[Assertion] = {
        val memCachedService = mock[MemCachedService]
        val cacheService = spy(new CacheServiceImpl(memCachedService))

        when(
          memCachedService.client
        ).thenReturn(memCachedClient)
        Option(memCachedClient).map { memCachedClient =>
          when(
            memCachedClient.set(anyString,anyInt,any)
          ).thenReturn(mock[OperationFuture[java.lang.Boolean]])
        }

        doReturn(Task.succeed(true)).when(cacheService).toTask(any[OperationFuture[java.lang.Boolean]])

        Runtime.default.unsafeRunToFuture(
          test(cacheService,memCachedClient)
        )
      }

      "save value to memcached if memcached is available" in {
        TestFixture() { (cacheService,memCachedClient) =>
          for {
            result <- cacheService.set(key,value)
          } yield {
            verify(memCachedClient, times(1)).set(anyString,anyInt,any)
            cacheService.cache.get(key) shouldBe None
            result shouldBe true
          }
        }
      }

      "save value to internal cache if memcached is not available" in {
        TestFixture(null) { (cacheService,memCachedClient) =>
          for {
            result <- cacheService.set(key,value)
          } yield {
            cacheService.cache.get(key) shouldBe Some(value)
            result shouldBe true
          }
        }
      }

    }

    "toTask" should {

      def TestFixture(
        result: Boolean,
        status: Boolean = true
      ): Future[Assertion] = {
        val connectionFactory = new net.spy.memcached.DefaultConnectionFactory()
        val latch = new java.util.concurrent.CountDownLatch(1)
        val operationFuture = spy(new OperationFuture[java.lang.Boolean]("noop",latch, 2500, connectionFactory.getListenerExecutorService()))

        doReturn(true).when(operationFuture).isDone()
        doReturn(false).when(operationFuture).isCancelled()

        val memCachedService = mock[MemCachedService]
        val cacheService = spy(new CacheServiceImpl(memCachedService))
        val task = cacheService.toTask(operationFuture)
        operationFuture.set(result, new net.spy.memcached.ops.OperationStatus(status,""))
        latch.countDown()

        Runtime.default.unsafeRunToFuture(task).map { result =>
          if(status) {
            result shouldBe result
          } else {
            fail("OperationFuture should have failed")
          }
        }.recover {
          case ex =>
            if(status) {
              fail("OperationFuture should have succeeded")
            } else {
              succeed
            }
        }
      }

      "convert OperationFuture to Task and return true" in {
        TestFixture(true)
      }

      "convert OperationFuture to Task and return false" in {
        TestFixture(false)
      }

      "convert OperationFuture to Task and fail" in {
        TestFixture(true,false)
      }

    }

  }

}
