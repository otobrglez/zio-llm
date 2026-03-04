package zio.llm.openrouter

import zio._
import zio.json.JsonCodec
import zio.json.ast.Json
import zio.llm.openrouter.Tools.{ToolCall, ToolResult}
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.{JsonCodec => SchemaJsonCodec}

object Tools {
  type ToolResult = String

  final case class ToolCall(name: String, args: String)
  object ToolCall {
    implicit val schema: Schema[ToolCall]               = DeriveSchema.gen[ToolCall]
    implicit val toolCallJSONCodec: JsonCodec[ToolCall] = SchemaJsonCodec.jsonCodec(schema)
  }

  final class Tool[A, R] private (
    val name: String,
    val description: Option[String],
    val argSchema: Schema[A],
    val argsCodec: JsonCodec[A],
    val handler: A => ZIO[R, Throwable, ToolResult],
  ) {

    def toOpenRouterJson: Json = Json.Obj(
      Map(
        "name"        -> Some(Json.Str(name)),
        "description" -> description.map(Json.Str(_)),
        "parameters"  -> JsonSchemaFromZIOSchema.encode(argSchema),
      ).collect { case (k, Some(v)) => k -> v }.toSeq: _*,
    )

    def runJSON() = ???
  }

  object Tool {
    final class Builder[A, R](
      val name: String,
      val description: Option[String] = None,
    )(implicit
      val argSchema: Schema[A],
      val argsCodec: JsonCodec[A],
    ) {
      def handle(f: A => ZIO[R, Throwable, ToolResult]): Tool[A, R] =
        new Tool(name, description, argSchema, argsCodec, f)
    }

    def define[A: Schema: JsonCodec, R](name: String)                      = new Builder[A, R](name)
    def define[A: Schema: JsonCodec, R](name: String, description: String) = new Builder[A, R](name, Some(description))
  }

}

final class Toolkit[R](
  private val tools: Map[String, Tools.Tool[?, R]],
) {
  def toOpenRouterToolsArrayJson: Json =
    Json.Arr(tools.values.map(_.toOpenRouterJson).toSeq: _*)

  def run(call: ToolCall) = tools.get(call.name) match {
    case None       => ZIO.fail(new IllegalArgumentException(s"""Unknown tool \"${call.name}\""""))
    case Some(tool) =>
      println(s"tool --> ${tool.name} / ${call.args}")
      ZIO.succeed(())
  }

  /*
  def run(call: ToolCall) =
    byName(call.name) match {
      case None       => ZIO.fail(new IllegalArgumentException(s"Unknown tool ${call.name}"))
      case Some(tool) => ???
    } */
}

object Toolkit {
  def apply[R](tools: Tools.Tool[?, R]*): Toolkit[R] = new Toolkit[R](
    Map(tools.map(tool => tool.name -> tool): _*),
  )
}
