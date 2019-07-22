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

import scala.quoted._
import scala.tasty._

object RecorderMacro {
  implicit val toolbox: scala.quoted.Toolbox = scala.quoted.Toolbox.make(getClass.getClassLoader)

  def apply(
      recording: Expr[Boolean],
      listener: Expr[RecorderListener[Boolean]])(implicit reflect: Reflection): Expr[Unit] = {
    apply(recording, '{""}, listener)
  }

  def apply(
      recording: Expr[Boolean],
      message: Expr[String],
      listener: Expr[RecorderListener[Boolean]])(implicit reflect: Reflection): Expr[Unit] = {
    import reflect._
    val termArg: Term = recording.unseal.underlyingArgument

    def getText(expr: Tree): String = {
      val pos = expr.pos
      (" " * pos.startColumn) + pos.sourceCode
    }

    '{
      val recorderRuntime: RecorderRuntime = new RecorderRuntime($listener)
      recorderRuntime.recordMessage($message)
      ${
        val runtimeSym = '[RecorderRuntime].unseal.symbol match {
          case IsClassDefSymbol(sym) => sym
        }
        val recordExpressionSel: Term = {
          val m = runtimeSym.method("recordExpression").head
          '{ recorderRuntime }.unseal.select(m)
        }
        val recordValueSel: Term = {
          val m = runtimeSym.method("recordValue").head
          '{ recorderRuntime }.unseal.select(m)
        }

        def recordExpressions(recording: Term): List[Term] = {
          val text = getText(recording)
          val ast = recording.showExtractors
          try {
            List(
              '{ recorderRuntime.resetValues() }.unseal,
              recordExpression(text, ast, recording)
            )
          } catch {
            case e: Throwable => throw new RuntimeException(
              "Expecty: Error rewriting expression.\nText: " + text + "\nAST : " + ast, e)
          }
        }

        def recordExpression(text: String, ast: String, expr: Term): Term = {
          val instrumented = recordAllValues(expr)
          Apply(recordExpressionSel,
            List(
              Literal(Constant.String(text)),
              Literal(Constant.String(ast)),
              instrumented
            ))
        }

        def recordAllValues(expr: Term): Term =
          expr match {
            case New(_)     => expr
            case Literal(_) => expr
            case Typed(r @ Repeated(xs, y), tpe) => recordSubValues(r)
            // don't record value of implicit "this" added by compiler; couldn't find a better way to detect implicit "this" than via point
            case Select(x@This(_), y) if expr.pos.start == x.pos.start => expr
            // case x: Select if x.symbol.isModule => expr // don't try to record the value of packages
            case _ => recordValue(recordSubValues(expr), expr)
          }

        def recordSubValues(expr: Term): Term =
          expr match {
            case Apply(x, ys) =>
              try {
                Apply(recordAllValues(x), ys.map(recordAllValues))
              } catch {
                case e: AssertionError => expr
              }
            // case TypeApply(x, ys) => recordValue(TypeApply.copy(expr)(recordSubValues(x), ys), expr)
            case TypeApply(x, ys) => TypeApply.copy(expr)(recordSubValues(x), ys)
            case Select(x, y)     => Select.copy(expr)(recordAllValues(x), y)
            case Typed(x, tpe)    => Typed.copy(expr)(recordSubValues(x), tpe)
            case Repeated(xs, y)  => Repeated.copy(expr)(xs.map(recordAllValues), y)
            case _                => expr
          }

        def recordValue(expr: Term, origExpr: Term): Term = {
          // debug
          // println("recording " + expr.showExtractors + " at " + getAnchor(expr))

          def skipIdent(sym: Symbol): Boolean =
            sym.fullName match {
              case "scala" | "java" => true
              case fullName if fullName.startsWith("scala.") => true
              case fullName if fullName.startsWith("java.")  => true
              case _ => false
            }

          def skipSelect(sym: Symbol): Boolean =
            (sym match {
              case IsDefDefSymbol(sym) => sym.signature.paramSigs.nonEmpty
              case IsValDefSymbol(sym) => skipIdent(sym)
              case _ => true
            })
          expr match {
            case Select(_, _) if skipSelect(expr.symbol) => expr
            case TypeApply(_, _) => expr
            case Ident(_) if skipIdent(expr.symbol) => expr
            case _ =>
              val tapply = recordValueSel.appliedToType(expr.tpe)
              Apply.copy(expr)(
                tapply,
                List(
                  expr,
                  Literal(Constant.Int(getAnchor(expr)))
                )
              )
          }
        }

        def getAnchor(expr: Term): Int =
          expr match {
            case Apply(x, ys) if x.symbol.fullName == "com.eed3si9n.expecty.RecorderRuntime.recordValue" && ys.nonEmpty =>
              getAnchor(ys.head)
            case Apply(x, ys)     => getAnchor(x) + 0
            case TypeApply(x, ys) => getAnchor(x) + 0
            case Select(x, y)     =>
              expr.pos.startColumn + math.max(0, expr.pos.sourceCode.indexOf(y))
            case _                => expr.pos.startColumn
          }
        Block(
          recordExpressions(termArg),
          '{ recorderRuntime.completeRecording() }.unseal
        ).seal
      }
      ()
    }
  }

  // 0.17.0 nightly
  // def apply(expr: Expr[Boolean]) given (qctx: QuoteContext): Expr[Unit] = {
  //   import qctx.tasty._
  //   import util._
  //   val termArg: Term = expr.unseal.underlyingArgument
  //   val errorMessage: Expr[Any] = Literal(Constant(termArg.toString)).seal
  //   '{
  //     if (!$expr) {
  //       throw new AssertionError(s"failed assertion: ${ $errorMessage }")
  //     }
  //   }
  // }
}
