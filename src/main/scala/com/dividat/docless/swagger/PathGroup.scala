package com.dividat.docless.swagger

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.instances.all._
import cats.syntax.eq._
import cats.syntax.foldable._
import cats.syntax.monoid._
import cats.{Eq, Monoid}
import com.dividat.docless.schema.JsonSchema.{Definition, TypeRef}

trait PathGroup {
  def params: List[OperationParameter] = Nil

  def definitions: List[Definition]

  def paths: List[Path]
}

object PathGroup {
  val Empty = PathGroup(Nil, Nil, Nil)

  def aggregate(
      info: Info,
      groups: List[PathGroup],
      securitySchemes: List[SecurityScheme] = Nil
  ): ValidatedNel[SchemaError, APISchema] = {
    val g          = groups.combineAll
    val allDefs    = g.definitions
    val definedIds = allDefs.map(_.id).toSet
    val securityDefinitions = SecurityDefinitions(securitySchemes: _*)

    def isDefined(ctx: RefWithContext): Boolean =
      allDefs.exists(_.id === ctx.ref.id)

    val missingDefinitions =
      allDefs.foldMap { d =>
        d.relatedRefs.collect {
          case r @ TypeRef(id, _) if !definedIds.exists(_ === id) =>
            SchemaError.missingDefinition(RefWithContext.definition(r, d))
        }
      }

    val errors =
      g.paths
        .foldMap(_.refs.filterNot(isDefined))
        .map(SchemaError.missingDefinition)
        .toList ++ missingDefinitions

    if (errors.nonEmpty)
      Validated.invalid[NonEmptyList[SchemaError], APISchema](
        NonEmptyList.fromListUnsafe(errors)
      )
    else
      Validated.valid {
        APISchema(
          info = info,
          host = "http://example.com/",
          basePath = "/",
          parameters = OperationParameters(g.params),
          paths = Paths(g.paths),
          schemes = Set(Scheme.Http),
          consumes = Set("application/json"),
          produces = Set("application/json"),
          securityDefinitions = securityDefinitions
        ).defining(g.definitions: _*)
      }
  }

  def apply(ps: List[Path],
            defs: List[Definition],
            _params: List[OperationParameter]): PathGroup =
    new PathGroup {

      override val paths       = ps
      override val definitions = defs
      override val params      = _params
    }

  implicit val pgEq: Eq[PathGroup] = Eq.fromUniversalEquals

  implicit def pgMonoid: Monoid[PathGroup] = new Monoid[PathGroup] {
    override def empty: PathGroup = Empty

    override def combine(x: PathGroup, y: PathGroup): PathGroup =
      PathGroup(
        x.paths |+| y.paths,
        x.definitions |+| y.definitions,
        x.params |+| y.params
      )
  }

}
