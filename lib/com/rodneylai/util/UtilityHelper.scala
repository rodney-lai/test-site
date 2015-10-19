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

package com.rodneylai.util

import java.util.{Calendar,Date}

object UtilityHelper
{
  def getDateAgoString(date:Date):String = {
    val timeDiff = Calendar.getInstance.getTime.getTime - date.getTime

    if (timeDiff < 1 * 1000) {
      "Less than a second ago"
    } else if (timeDiff < 1.5 * 1000) {
      "About a second ago"
    } else if (timeDiff <= 180 * 1000) {
      (timeDiff/1000).toInt.toString + " seconds ago"
    } else if (timeDiff <= 120 * 60 * 1000) {
      (timeDiff/(60*1000)).toInt.toString + " minutes ago"
    } else if (timeDiff <= 72 * 60 * 60 * 1000) {
      (timeDiff/(60 * 60 * 1000)).toInt.toString + " hours ago"
    } else {
      (timeDiff/(24 * 60 * 60 * 1000)).toInt.toString + " days ago"
    }
  }
}
