package zio.llm.openrouter

import zio._
import zio.test._

object OpenRouterSpec extends ZIOSpecDefault {
  def spec: Spec[TestEnvironment with Scope, Throwable] = suite("OpenRouterSpec")(
    test("completions") {
      for {
        _ <-
          OpenRouter
            .completions(
              model = "openai/gpt-5-chat",
              prompt = Some("Who was John Lennon?"),
              usage = Some(Completions.Usage.include),
              temperature = Some(0.0),
              reasoning = Some(Completions.Reasoning(effort = Some(Completions.Effort.High))),
            )
            .tap(l => zio.Console.print(l.choices.map(_.text.get).mkString("")))
            .runDrain

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
