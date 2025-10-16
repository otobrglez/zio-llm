package zio.llm.openrouter

import zio.json._
import zio.http._
import zio.llm.openrouter.Completions.{Provider, Reasoning, Usage}
import zio.llm.{Model, Prompt}
import zio.schema.{DeriveSchema, Schema}
import zio.stream.ZStream
import zio.{Console, RIO, RLayer, Scope, TaskLayer, ZIO, ZLayer}
import zio.schema.annotation.caseName

sealed trait Error                                                              extends Throwable {
  def message: String
  override def getMessage: String = message
}
final case class BadConnectError(err: String)                                   extends Error     {
  def message = s"Failed to connect to OpenRouter - ${err}"
}
final case class BadResponseError(status: Status, responseBody: Option[String]) extends Error     {
  def message = s"Failed to connect to OpenRouter ($status): \"${responseBody.getOrElse("<empty>")}\""
}
final case class ChunkHandlingError(err: String)                                extends Error     {
  def message = s"Failed to handle chunk - ${err}"
}

object Completions {
  sealed trait Effort
  object Effort {
    @caseName("low") case object Low       extends Effort
    @caseName("medium") case object Medium extends Effort
    @caseName("high") case object High     extends Effort
    implicit val schema: Schema[Effort] = DeriveSchema.gen[Effort]
  }

  final case class Provider(sort: String)
  final case class Reasoning(
    effort: Option[Effort] = None,
    `max_tokens`: Option[Int] = None,
    exclude: Option[Boolean] = None,
  )

  final case class Usage(include: Option[Boolean] = None)
  object Usage { val include = Usage(Some(true)); }
}

final class OpenRouter private (
  private val client: Client,
  private val config: OpenRouterConfig,
) {
  import Completions._

  def completions(
    model: Model,
    prompt: Prompt,
    models: Option[List[Model]] = None,
    provider: Option[Provider] = None,
    reasoning: Option[Reasoning] = None,
    usage: Option[Usage] = None,
    transforms: Option[List[String]] = None,
    // stream: Boolean = false,
    maxTokens: Option[Int] = None,
    temperature: Option[Double] = None,
    seed: Option[Int] = None,
    topP: Option[Double] = None,
    topK: Option[Int] = None,
    frequencyPenalty: Option[Double] = None,
    repetitionPenalty: Option[Double] = None,
    logitBias: Option[Map[String, Double]] = None,
    topLogprobs: Option[Int] = None,
    minP: Option[Double] = None,
    topA: Option[Int] = None,
    user: Option[String] = None,
  ): ZStream[Scope, Throwable, CompletionsResponse.Chunk] = for {
    _ <- ZStream.unit
    // format: off
    requestPayload = CompletionsRequest(
      model, prompt, models, provider, reasoning, usage, transforms, stream = true,
      maxTokens, temperature, seed, topP, topK, frequencyPenalty, repetitionPenalty,
      logitBias, topLogprobs, minP, topA, user,
    )
    // format: on
    _ <- ZStream.logDebug(s"Requesting with ${requestPayload.toJsonPretty}")

    request = Request.post("chat/completions", Body.fromString(requestPayload.toJson))
    response <- ZStream.fromZIO(client.request(request).mapError(e => BadConnectError(e.getMessage)))

    _ <- ZStream.fromZIO(
      ZIO.when(response.status != Status.Ok) {
        response.body
          .asString(Charsets.Utf8)
          .flatMap { errorBody => ZIO.fail(BadResponseError(response.status, Some(errorBody))) }
          .catchAll { _ => ZIO.fail(BadResponseError(response.status, None)) }
      },
    )

    chunk <-
      response.body
        .asServerSentEvents[String]
        .map(CompletionsResponse.fromServerSideEvent)
        .flatMap {
          case Left(err)                               => ZStream.fail(ChunkHandlingError(err))
          case Right(chunk: CompletionsResponse.Chunk) => ZStream.succeed(chunk)
          case Right(CompletionsResponse.StreamDone)   => ZStream.empty
        }
  } yield chunk
}

object OpenRouter {
  private def mkLayer: RIO[Client with OpenRouterConfig, OpenRouter] = for {
    baseURL <- ZIO.fromEither(URL.decode("https://openrouter.ai/api/v1/"))
    config  <- ZIO.service[OpenRouterConfig]
    client  <- ZIO.serviceWith[Client](
      _.url(baseURL).addHeader("Content-Type", "application/json").addHeaders(config.toHeaders),
    )
  } yield new OpenRouter(client, config)

  def liveNoClient: RLayer[Client with OpenRouterConfig, OpenRouter]  = ZLayer.fromZIO(mkLayer)
  def live: RLayer[OpenRouterConfig, OpenRouter]                      = Client.default >>> OpenRouter.liveNoClient
  def liveWithConfig(config: OpenRouterConfig): TaskLayer[OpenRouter] = ZLayer.succeed(config) >>> OpenRouter.live

  def completions(
    model: Model,
    prompt: Prompt,
    models: Option[List[Model]] = None,
    provider: Option[Provider] = None,
    reasoning: Option[Reasoning] = None,
    usage: Option[Usage] = None,
    transforms: Option[List[String]] = None,
    maxTokens: Option[Int] = None,
    temperature: Option[Double] = None,
    seed: Option[Int] = None,
    topP: Option[Double] = None,
    topK: Option[Int] = None,
    frequencyPenalty: Option[Double] = None,
    repetitionPenalty: Option[Double] = None,
    logitBias: Option[Map[String, Double]] = None,
    topLogprobs: Option[Int] = None,
    minP: Option[Double] = None,
    topA: Option[Int] = None,
    user: Option[String] = None,
  ) = ZStream.serviceWithStream[OpenRouter](
    _.completions(
      model,
      prompt,
      models,
      provider,
      reasoning,
      usage,
      transforms,
      maxTokens,
      temperature,
      seed,
      topP,
      topK,
      frequencyPenalty,
      repetitionPenalty,
      logitBias,
      topLogprobs,
      minP,
      topA,
      user,
    ),
  )
}

final case class OpenRouterConfig(
  apiKey: String,
  httpReferer: Option[String] = None,
  xTitle: Option[String] = None,
) {
  val toHeaders: Seq[(String, String)] = Seq(
    "Authorization" -> Some(s"Bearer $apiKey"),
    "HTTP-Referer"  -> httpReferer,
    "X-Title"       -> xTitle,
  ).collect { case (k, Some(v)) => (k, v) }
}
