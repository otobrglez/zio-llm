# ZIO-LLM

The [`zio-llm`][zio-llm] is a Scala 2.13/3+ library and a streaming wrapper for popular LLMs.

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.otobrglez/zio-llm_3)](https://repo1.maven.org/maven2/io/github/otobrglez/zio-llm_3/)
[![ZIO LLM](https://img.shields.io/github/stars/otobrglez/zio-llm?style=social)](https://github.com/otobrglez/zio-llm)

This project is in the early stages of development, and contributions are welcome!

## Installing

```scala
libraryDependencies += "io.github.otobrglez" %% "zio-llm"            % "0.0.1"
libraryDependencies += "io.github.otobrglez" %% "zio-llm-openrouter" % "0.0.1"
```

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

## Development

Please consider using [devenv](https://devenv.sh/) for local development.

To run tests, make sure you have valid local environment variables set.

```bash
export OPENROUTER_API_KEY="<openrouter-api-key>"
export OPENAI_API_KEY="<openai-api-key>"

sbt test
```

## Releasing

```sbt
sbt +clean +test
sbt +publishSigned
sbt sonatypeBundleRelease
```

## Authors

- [Oto Brglez](https://github.com/otobrglez)

[zio-llm]: https://github.com/otobrglez/zio-llm
