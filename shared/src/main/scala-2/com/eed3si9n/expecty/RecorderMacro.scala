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

import compat._
import scala.util.Properties

class RecorderMacro[C <: Context](val context: C) {
  import context.universe._

  def apply[R: context.WeakTypeTag, A: context.WeakTypeTag](recording: context.Tree, message: context.Tree): Expr[A] = {
    context.Expr(Block(declareRuntime[R, A] ::
      recordMessage(message) ::
      recordExpressions(recording),
      completeRecording))
  }

  def all[R: context.WeakTypeTag, A: context.WeakTypeTag](recordings: Seq[context.Tree]): Expr[A] = {
    context.Expr(Block(
      declareRuntime[R, A] ::
      recordings.toList.flatMap(recordExpressions),
      completeRecording))
  }

  private[this] def declareRuntime[R: context.WeakTypeTag, A : context.WeakTypeTag] : Tree = {
    val runtimeClass = context.mirror.staticClass(classOf[RecorderRuntime[_, _]].getName())
    ValDef(
      Modifiers(),
      termName(context)("$com_eed3si9n_expecty_recorderRuntime"),
      TypeTree(weakTypeOf[RecorderRuntime[R, A]]),
      Apply(
        Select(
          New(Ident(runtimeClass)),
          termNames.CONSTRUCTOR),
        List(
          Select(
            context.prefix.tree,
            termName(context)("listener")))))
  }

  private[this] def recordExpressions(recording: Tree): List[Tree] = {
    val text = getText(recording)
    val ast = showRaw(recording)
    try {
      List(resetValues, recordExpression(text, ast, recording))
    } catch {
      case e: Throwable => throw new RuntimeException(
        "Expecty: Error rewriting expression.\nText: " + text + "\nAST : " + ast, e)
    }
  }

  private[this] def recordMessage(message: Tree): Tree =
    Apply(
      Select(
        Ident(termName(context)("$com_eed3si9n_expecty_recorderRuntime")),
        termName(context)("recordMessage")),
      List(message))

  private[this] def completeRecording: Tree =
    Apply(
      Select(
        Ident(termName(context)("$com_eed3si9n_expecty_recorderRuntime")),
        termName(context)("completeRecording")),
      List())


  private[this] def resetValues: Tree =
    Apply(
      Select(
        Ident(termName(context)("$com_eed3si9n_expecty_recorderRuntime")),
        termName(context)("resetValues")),
      List())

  private[this] def recordExpression(text: String, ast: String, expr: Tree) = {
    val instrumented = recordAllValues(expr)
    log(expr, s"""
Expression      : ${text.trim()}
Original AST    : $ast
Instrumented AST: ${showRaw(instrumented)}")

    """)

    Apply(
      Select(
        Ident(termName(context)("$com_eed3si9n_expecty_recorderRuntime")),
        termName(context)("recordExpression")),
      List(
        context.literal(text).tree,
        context.literal(ast).tree,
        instrumented,
        getSourceLocation))
  }

  private[this] def splitExpressions(recording: Tree): List[Tree] = recording match {
    case Block(xs, y) => xs ::: List(y)
    case _            => List(recording)
  }

  private[this] def recordAllValues(expr: Tree): Tree = expr match {
    case New(_) => expr // only record after ctor call
    case Literal(_) => expr // don't record
    // don't record value of implicit "this" added by compiler; couldn't find a better way to detect implicit "this" than via point
    case Select(x@This(_), y) if getPosition(expr).point == getPosition(x).point => expr
    case x: Select if x.symbol.isModule => expr // don't try to record the value of packages
    case _ => recordValue(recordSubValues(expr), expr)
  }

  private[this] def recordSubValues(expr: Tree) : Tree = expr match {
    case Apply(x, ys) => Apply(recordAllValues(x), ys.map(recordAllValues))
    case TypeApply(x, ys) => recordValue(TypeApply(recordSubValues(x), ys), expr)
    case Select(x, y) => Select(recordAllValues(x), y)
    case _ => expr
  }

  private[this] def recordValue(expr: Tree, origExpr: Tree): Tree =
    if (origExpr.tpe.typeSymbol.isType)
      Apply(
        Select(
          Ident(termName(context)("$com_eed3si9n_expecty_recorderRuntime")),
          termName(context)("recordValue")),
        List(expr, Literal(Constant(getAnchor(origExpr)))))
    else expr

  private[this] def getText(expr: Tree): String = getPosition(expr).lineContent

  private[this] def getAnchor(expr: Tree): Int = expr match {
    case Apply(x, ys) => getAnchor(x) + 0
    case TypeApply(x, ys) => getAnchor(x) + 0
    case _ => {
      val pos = getPosition(expr)
      pos.point - pos.source.lineToOffset(pos.line - 1)
    }
  }

  private[this] def getPosition(expr: Tree) = expr.pos.asInstanceOf[scala.reflect.internal.util.Position]

  private[this] def log(expr: Tree, msg: => String): Unit = {
    if (Properties.propOrFalse("org.expecty.debug")) context.info(expr.pos, msg, force = false)
  }

  private def getSourceLocation = {
    import context.universe._

    val pwd  = java.nio.file.Paths.get("").toAbsolutePath
    val p = context.enclosingPosition.source.path
    val abstractFile = context.enclosingPosition.source.file

    val rp = if (!abstractFile.isVirtual){
      pwd.relativize(abstractFile.file.toPath())
    } else p

    val path = Literal(Constant(p))
    val relativePath = Literal(Constant(rp))
    val line = Literal(Constant(context.enclosingPosition.line))
    New(typeOf[Location], path, relativePath, line)
  }

}

object VarargsRecorderMacro {
  def apply[R: context.WeakTypeTag, A: context.WeakTypeTag](context: Context)(recordings: context.Tree*): context.Expr[A] = {
    new RecorderMacro[context.type](context).all[R, A](recordings)
  }
}

object RecorderMacro1 {
  def apply[R: context.WeakTypeTag, A: context.WeakTypeTag](context: Context)(recording: context.Tree): context.Expr[A] = {
    new RecorderMacro[context.type](context).apply[R, A](recording, context.literal("").tree)
  }
}

object RecorderMacro {
  def apply[R: context.WeakTypeTag, A : context.WeakTypeTag](context: Context)(recording: context.Tree, message: context.Tree): context.Expr[A] = {
    new RecorderMacro[context.type](context).apply[R, A](recording, message)
  }
}
