//     Project: angulate2 (https://github.com/jokade/angulate2)
// Description: Angulate2 extension for simplified creation of routing modules

// Copyright (c) 2016 Johannes.Kastner <jokade@karchedon.de>
//               Distributed under the MIT License (see included LICENSE file)
package angulate2.ext

import angulate2.internal.ClassDecorator
import angulate2.router.Route

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.scalajs.js

@compileTimeOnly("enable macro paradise to expand macro annotations")
class Routes(root: Boolean, routes: Route*) extends StaticAnnotation {
  def this(providers: js.Array[js.Any])(root: Boolean, routes: Route*) = this(root,routes:_*)
  def macroTransform(annottees: Any*): Any = macro Routes.Macro.impl
}

object Routes {
  private[angulate2] class Macro(val c: whitebox.Context) extends ClassDecorator {
    import c.universe._

    val annotationParamNames = Seq(
      "root",
      "routes*"
    )

    override val firstArglistParamNames = Seq(
      "providers"
    )

    override val annotationName: String = "Routes"

    override def mainAnnotationObject = q"angulate2.core.NgModuleFacade"

    private val routerModule = q"angulate2.ops.@@[angulate2.router.RouterModule]"

    override def analyze: Analysis = super.analyze andThen {
      case (cls: ClassParts, data) =>
        val cdd = ClassDecoratorData(data)
        val routes = q"scalajs.js.Array(..${cdd.annotParams("routes")})"
        val remainingParams = cdd.annotParams - "root" - "routes"

        val imports = cdd.annotParams("root") match {
          case Literal(Constant(true)) => q"scalajs.js.Array(angulate2.router.RouterModule.forRoot($routes))"
          case Literal(Constant(false)) => q"scalajs.js.Array(angulate2.router.RouterModule.forChild($routes))"
        }
        (cls, ClassDecoratorData.update(data,cdd.copy(annotParams = remainingParams ++ Map("imports" -> imports, "exports" -> routerModule))))
      case default => default
    }

  }
}
