/**
 *
 * Copyright (c) 2015-2020 Rodney S.K. Lai
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

package com.rodneylai.util

import scala.collection.JavaConverters
import scala.collection.JavaConverters._
import scala.collection.mutable.Buffer

class ConversionHelperImpl extends ConversionHelper
{
   def asScalaBuffer[A](list: java.util.List[A]): Buffer[A] = {
     JavaConverters.asScalaBuffer(list)
   }

   def enumerationAsScalaIterator[A](enumeration: java.util.Enumeration[A]): Iterator[A] = {
     JavaConverters.enumerationAsScalaIterator(enumeration)
   }

   def asJavaList[A](list: List[A]): java.util.List[A] = {
     list.asJava
   }
}
