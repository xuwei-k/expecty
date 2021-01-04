/*
* Copyright 2021 the original author or authors.
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
      lazy val rendering: String = new ExpressionRenderer(showTypes = showTypes, shortString = false).render(recordedExpr)
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
        recording: Recording[Boolean], recordedMessage: Function0[String]) = {

      if(recording.recordedExprs.exists(e => !e.value) && !failEarly){
        val failedExprs = recording.recordedExprs.filterNot(_.value).reverse

        val loc = failedExprs.head.location
        val locStr = if(showLocation) " (" + loc.relativePath + ":" + loc.line + ")" else ""
        val msg = recordedMessage()

        val rendering = failedExprs
          .map(new ExpressionRenderer(showTypes = showTypes, shortString = false).render)
          .mkString("\n")

        val assertion = if (failedExprs.size > 1) "assertions" else "assertion"

        val header =
          assertion + " failed " + locStr +
            (if (msg == "") ""
            else ": " + msg)
        throw new AssertionError(header + "\n\n" + rendering)
      }
    }
  }

  override lazy val listener = new ExpectyListener
}

trait StringAssertEquals extends AssertEquals[Unit] {
  class StringAssertEqualsListener extends RecorderListener[String, Unit] {
    val showTypes: Boolean = false
    override def recordingCompleted(recording: Recording[String], recordedMessage: Function0[String]) = {
      recording.recordedExprs match {
        case expected :: found :: Nil =>
          if (expected.value == found.value) ()
          else {
            lazy val rendering: String = new ExpressionRenderer(showTypes = false, shortString = true).render(found)
            val msg = recordedMessage()
            val header =
              "assertion failed" +
                (if (msg == "") ""
                 else ": " + msg)

            val expectedLines = expected.value.linesIterator.toSeq
            val foundLines = found.value.linesIterator.toSeq
            val diff = DiffUtil
              .mkColoredLineDiff(expectedLines, foundLines)
              .linesIterator
              .toSeq
              .map(str => Console.RESET.toString + str)
              .mkString("\n")
            throw new AssertionError(header + "\n\n" + rendering + diff)
          }
        case _ => throw new RuntimeException("unexpected number of expressions " + recording)
      }
    }
  }

  override lazy val stringAssertEqualsListener: RecorderListener[String, Unit] =
    new StringAssertEqualsListener
}

class Expecty() extends ExpectyBase with UnaryRecorder[Boolean, Unit]
class VarargsExpecty() extends ExpectyBase with VarargsRecorder[Boolean, Unit]

object Expecty extends StringAssertEquals {
  lazy val assert: Expecty = new Expecty()
  lazy val expect: VarargsExpecty = new VarargsExpecty()
}
