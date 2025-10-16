# ZIO-LLM

The [`zio-llm`][zio-llm] is a Scala 2.13/3+ library and a streaming wrapper for popular LLMs.

This project is in the early stages of development, and contributions are welcome!

## OpenRouter

### Basic usage with "chat completions"

```scala
import zio.llm.openrouter._

// Few things omitted for brevity.

OpenRouter.completions(
    model = "openai/gpt-5-chat",
    prompt = "Who was John Lennon?",
  )
  .tap(l => Console.print(l.choices.map(_.text.get).mkString("")))
  .runDrain
```

### Configuration

Configuration and further tuning should be done via `OpenRouterConfig`. Either via the `Layer` composition or function
calling.

```scala
OpenRouter.completions(/* ... */)
  .provide(
    OpenRouter.liveWithConfig(
      OpenRouterConfig(
        apiKey = "my-key"
      )
    )
  )

// Or via Layers.

OpenRouter.completions(/* ... */)
  .provide(
    ZLayer.succeed(OpenRouterConfig(
      apiKey = "my-key"
    )) >> OpenRouter.live
  )
```

See [`zio-llm-openrouter` examples](zio-llm-openrouter/src/main/scala/zio/llm/openrouter/examples) folder for more
use-cases.

## Author

- [Oto Brglez](https://github.com/otobrglez)

[zio-llm]: https://github.com/otobrglez/zio-llm
