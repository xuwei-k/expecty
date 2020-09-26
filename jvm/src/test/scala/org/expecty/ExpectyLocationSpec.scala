/*
* Copyright 2012 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*     http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package foo

import org.junit.Assert._
import org.junit.Test
import junit.framework.ComparisonFailure
import com.eed3si9n.expecty.Expecty

class ExpectyLocationSpec {
  val assert = new Expecty { override val showLocation = true }

  @Test
  def position(): Unit = {
    outputs("""assertion failed (jvm/src/test/scala/org/expecty/ExpectyLocationSpec.scala:32)

"abc".length() == 2
      |        |
      3        false
    """) {
      assert {
        "abc".length() == 2
      }
    }
  }


  def outputs(rendering: String)(expectation: => Unit): Unit = {
    def normalize(s: String) = augmentString(s.trim()).lines.mkString

    try {
      expectation
      fail("Expectation should have failed but didn't")
    }
    catch  {
      case e: AssertionError => {
        val expected = normalize(rendering)
        val actual = normalize(e.getMessage).replaceAll("@[0-9a-f]*", "@\\.\\.\\.")
        if (actual != expected) {
          throw new ComparisonFailure(s"Expectation output doesn't match: ${e.getMessage}",
            expected, actual)
        }
      }
    }
  }


}

