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

package com.eed3si9n.expecty

abstract class ExpectyBase extends Recorder[Boolean, Unit] {
  val failEarly: Boolean = true
  val showTypes: Boolean = false
  val showLocation: Boolean = false
  // val printAsts: Boolean = false
  // val printExprs: Boolean = false

  class ExpectyListener extends RecorderListener[Boolean, Unit] {
    override def expressionRecorded(
        recordedExpr: RecordedExpression[Boolean], recordedMessage: Function0[String]): Unit = {
      lazy val rendering: String = new ExpressionRenderer(showTypes).render(recordedExpr)
      // if (printAsts) println(recordedExpr.ast + "\n")
      // if (printExprs) println(rendering)
      if (!recordedExpr.value && failEarly) {
        val loc = recordedExpr.location
        val locStr = if(showLocation) " (" + loc.relativePath + ":" + loc.line + ")" else ""
        val msg = recordedMessage()
        val header =
          "assertion failed" + locStr +
            (if (msg == "") ""
            else ": " + msg)
        throw new AssertionError(header + "\n\n" + rendering)
      }
    }

    override def recordingCompleted(
        recording: Recording[Boolean], recordedMessage: Function0[String]) = {}
  }

  override lazy val listener = new ExpectyListener
}

class Expecty() extends ExpectyBase with UnaryRecorder[Boolean, Unit]
class VarargsExpecty() extends ExpectyBase with VarargsRecoder[Boolean, Unit]

object Expecty {
  lazy val assert: Expecty = new Expecty()
  lazy val expect: VarargsExpecty = new VarargsExpecty()
}
