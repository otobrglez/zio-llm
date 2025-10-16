package zio.llm.openrouter.examples

import zio._
import zio.llm.openrouter._
import ZIO.fromOption

object Main extends ZIOAppDefault {
  private def program: RIO[Scope with OpenRouter with ZIOAppArgs, Unit] = for {
    args   <- getArgs
    prompt <- fromOption(args.headOption).orElseFail(new IllegalArgumentException("No prompt provided."))

    _ <-
      OpenRouter
        .completions(
          model = "openai/gpt-5-chat",
          prompt,
          temperature = Some(0.0),
        )
        .tap(l => Console.print(l.choices.map(_.text.get).mkString("")))
        .runDrain
  } yield ()

  def run: RIO[ZIOAppArgs, Unit] = program.provideSome[ZIOAppArgs](
    Scope.default,
    ZLayer.fromZIO(mkConfigLayer),
    OpenRouter.live,
  )

  private def mkConfigLayer: Task[OpenRouterConfig] = for {
    maybeApiKey <- System.env("OPENROUTER_API_KEY")
    apiKey <- fromOption(maybeApiKey).orElseFail(new IllegalArgumentException("No OPENROUTER_API_KEY key provided."))
  } yield OpenRouterConfig(apiKey)
}
