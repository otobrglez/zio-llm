package zio.llm.openrouter

import zio._
import zio.json.JsonCodec
import zio.test._
import zio.llm.openrouter.Tools.Tool
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.{JsonCodec => SchemaJsonCodec}

private[openrouter] object MyExampleTools {

  final case class GetWeatherArgs(city: String)
  object GetWeatherArgs {
    implicit val schema: Schema[GetWeatherArgs]       = DeriveSchema.gen
    implicit val jsonCodec: JsonCodec[GetWeatherArgs] = SchemaJsonCodec.jsonCodec(schema)
  }

  final case class GetWeatherDateArgs(city: String, date: Option[String])
  object GetWeatherDateArgs {
    implicit val schema: Schema[GetWeatherDateArgs]       = DeriveSchema.gen
    implicit val jsonCodec: JsonCodec[GetWeatherDateArgs] = SchemaJsonCodec.jsonCodec(schema)
  }

  final case class WeatherResponse(temperatureC: Double)
  object WeatherResponse {
    implicit val schema: Schema[WeatherResponse]       = DeriveSchema.gen
    implicit val jsonCodec: JsonCodec[WeatherResponse] = SchemaJsonCodec.jsonCodec(schema)
  }

  private val getWeather =
    Tool.define[GetWeatherArgs, WeatherResponse]("get_weather").handle { args =>
      zio.Console.printLine(s"Getting weather for ${args.city}").as("Sunny")
    }

  private val getWeatherNextWeek =
    Tool
      .define[GetWeatherDateArgs, WeatherResponse]("get_weather_for_date", "Gets weather for city for given date.")
      .handle { args =>
        zio.Console.printLine(s"Getting weather for ${args.city} next week").as("Sunny")
      }

  final case class SumNumbersArgs(a: Int, b: Int)
  object SumNumbersArgs {
    implicit val schema: Schema[SumNumbersArgs]       = DeriveSchema.gen
    implicit val jsonCodec: JsonCodec[SumNumbersArgs] = SchemaJsonCodec.jsonCodec(schema)
  }

  final case class SumNumbersResponse(sum: Int)
  object SumNumbersResponse {
    implicit val schema: Schema[SumNumbersResponse]       = DeriveSchema.gen
    implicit val jsonCodec: JsonCodec[SumNumbersResponse] = SchemaJsonCodec.jsonCodec(schema)
  }

  private val sumNumbers = Tool.define[SumNumbersArgs, SumNumbersResponse]("sum_numbers").handle { args =>
    ZIO.succeed(args.a + args.b).map(SumNumbersResponse.apply)
  }

  val tools = Toolkit(getWeather, getWeatherNextWeek)
}

object OpenRouterToolsSpec extends ZIOSpecDefault {
  def spec = suite("OpenRouterToolsSpec")(
    test("basic usage") {
      for {
        _ <- ZIO.unit
        _ = println("--- " * 10)

        exampleTools = MyExampleTools.tools

        //  .toOpenRouterToolsArrayJson
        // _ = println(tool.toOpenRouterToolsArrayJson.toJsonPretty)

        result <- exampleTools.run(new Tools.ToolCall("get_weather", """{"city":"Ljubljana"}"""))
        _ = println(result)

        _ <-
          ZIO.serviceWithZIO[OpenRouter](
            _.completionText(
              model = "google/gemini-2.5-flash",
              messages = Seq(
                Message.system(
                  """You are a helpful assistant named ZIOLLM. 
                    |You will use tools that are given to you.
                    |Provide short and quick answers.
                    |You can tell about weather.""".stripMargin,
                ),
                Message.developer("Greet user and tell them about tools."),
                Message.user("What is the weather like in Ljubljana?"),
              ),
              usage = Some(Completions.Usage.include),
              temperature = Some(0.0),
            ).tap(p => zio.Console.printLine(p)),
          )

      } yield assertCompletes
    },
  ).provideShared(
    Scope.default,
    mkConfigLayer,
    OpenRouter.live,
  ) @@ TestAspect.withLiveSystem @@ TestAspect.withLiveClock

  private def mkConfigLayer: TaskLayer[OpenRouterConfig] = ZLayer.fromZIO {
    for {
      maybeApiKey <- System.env("OPENROUTER_API_KEY")
      apiKey      <-
        ZIO
          .fromOption(maybeApiKey)
          .orElseFail(new IllegalArgumentException("No OPENROUTER_API_KEY key provided."))
    } yield OpenRouterConfig(apiKey)
  }
}
