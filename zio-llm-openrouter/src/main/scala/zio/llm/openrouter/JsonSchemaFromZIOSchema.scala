package zio.llm.openrouter

import zio.schema._
import zio.json._
import zio.json.ast.Json
import zio.schema.StandardType._

import scala.annotation.tailrec

object JsonSchemaFromZIOSchema {
  def encode[A](schema: Schema[A]): Option[Json] = encodeSchema(schema).toJsonAST.toOption

  private def str(s: String): Json               = Json.Str(s)
  private def obj(fields: (String, Json)*): Json = Json.Obj(fields: _*)

  private def encodePrimitive(st: StandardType[_]): Json = st match {
    case StringType     => obj("type" -> str("string"))
    case IntType        => obj("type" -> str("integer"))
    case LongType       => obj("type" -> str("integer"))
    case DoubleType     => obj("type" -> str("number"))
    case FloatType      => obj("type" -> str("number"))
    case BoolType       => obj("type" -> str("boolean"))
    case BigDecimalType => obj("type" -> str("number"))
    case BigIntegerType => obj("type" -> str("integer"))
    case _              => obj("type" -> str("string")) // fallback
  }

  private def encodeSchema[A](schema: Schema[A]): Json = schema match {
    case Schema.Primitive(st, _)   => encodePrimitive(st)
    case Schema.Lazy(schema0)      => encodeSchema(schema0())
    case Schema.Optional(inner, _) => encodeSchema(inner)

    case record: Schema.Record[A] =>
      val props: List[(String, Json)]       = record.fields.map { f => f.name -> encodeSchema(f.schema) }.toList
      val required                          = record.fields.collect { case f if !isOptional(f.schema) => f.name }
      obj(
        "type"       -> str("object"),
        "properties" -> Json.Obj(props: _*),
        "required"   -> Json.Arr(required.map(Json.Str(_)): _*),
      )

    case Schema.Sequence(elements, _, _, _, _) =>
      obj("type" -> str("array"), "items" -> encodeSchema(elements))

    case enum: Schema.Enum[A] =>
      obj(
        "type" -> str("string"),
        "enum" -> Json.Arr(enum.cases.map(c => Json.Str(c.id)): _*),
      )

    case Schema.Tuple2(left, right, _) =>
      obj(
        "type"        -> str("array"),
        "prefixItems" -> Json.Arr(encodeSchema(left), encodeSchema(right)),
        "minItems"    -> Json.Num(2),
        "maxItems"    -> Json.Num(2),
      )

    case Schema.Transform(inner, _, _, _, _)                     => encodeSchema(inner)
    case Schema.Fail(_, _) | Schema.Transform(_, _, _, _, _) | _ => obj("type" -> str("object"))
  }

  @tailrec
  private def isOptional[A](schema: Schema[A]): Boolean = schema match {
    case Schema.Optional(_, _) => true
    case Schema.Lazy(schema0)  => isOptional(schema0())
    case _                     => false
  }
}
