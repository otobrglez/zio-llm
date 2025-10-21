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
    implicit val schema: Schema[GetWeatherArgs]       = DeriveSchema.gen[GetWeatherArgs]
    implicit val jsonCodec: JsonCodec[GetWeatherArgs] = SchemaJsonCodec.jsonCodec(schema)
  }

  final case class GetWeatherDateArgs(city: String, date: Option[String])
  object GetWeatherDateArgs {
    implicit val schema: Schema[GetWeatherDateArgs]       = DeriveSchema.gen[GetWeatherDateArgs]
    implicit val jsonCodec: JsonCodec[GetWeatherDateArgs] = SchemaJsonCodec.jsonCodec(schema)
  }

  final case class WeatherResponse(temperatureC: Double)
  object WeatherResponse {
    implicit val schema: Schema[WeatherResponse]       = DeriveSchema.gen[WeatherResponse]
    implicit val jsonCodec: JsonCodec[WeatherResponse] = SchemaJsonCodec.jsonCodec(schema)
  }
  trait WeatherService   {
    def getWeather(args: GetWeatherArgs): Task[WeatherResponse]
  }
  object WeatherService  {
    def live: ULayer[WeatherService] = ZLayer.succeed(new WeatherService {
      def getWeather(args: GetWeatherArgs): Task[WeatherResponse] =
        ZIO.succeed(WeatherResponse(42.toDouble))
    })
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

  val tools = Toolkit(getWeather, getWeatherNextWeek)
}

object OpenRouterToolsSpec extends ZIOSpecDefault {
  def spec: Spec[TestEnvironment with Scope, Throwable] = suite("OpenRouterToolsSpec")(
    test("basic usage") {
      for {
        _ <- ZIO.unit
        _ = println("--- " * 10)

        tool = MyExampleTools.tools

        //  .toOpenRouterToolsArrayJson
        // _ = println(tool.toOpenRouterToolsArrayJson.toJsonPretty)

        result <- tool.run(new Tools.ToolCall("get_weather", """{"city":"Ljubljana"}"""))
        _ = println(result)

        _ <-
          ZIO.serviceWithZIO[OpenRouter](
            _.completionText(
              model = "google/gemini-2.5-flash",
              // model = "openai/gpt-4.1",
              messages = Seq(
                Message.system(
                  """You are a helpful assistant named ZIOLLM. 
                    |You will use tools that are given to you.
                    |Provide short and quick answers.
                    |You can tell about weather.""".stripMargin,
                ),
                // Message.developer("Introduce yourself and greet user!"),
                Message.user("Who are you? Do you know about Elton John?"),
              ),
              usage = Some(Completions.Usage.include),
              temperature = Some(0.0),
            ).tap(p => zio.Console.printLine(p)),
          )

      } yield assertCompletes
    },
  ).provideShared(
    Scope.default,
    ZLayer.fromZIO(mkConfigLayer),
    OpenRouter.live,
  ) @@ TestAspect.withLiveSystem @@ TestAspect.withLiveClock

  private def mkConfigLayer: Task[OpenRouterConfig] = for {
    maybeApiKey <- System.env("OPENROUTER_API_KEY")
    apiKey      <- ZIO.getOrFail(maybeApiKey)
  } yield OpenRouterConfig(apiKey)
}
