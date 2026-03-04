package zio.llm.openrouter

import zio.http.ServerSentEvent
import zio.schema.{DeriveSchema, Schema}
import zio.schema.annotation.fieldName

sealed trait CompletionsResponse

object CompletionsResponse {
  final case class Chunk(
    id: String,
    provider: String,
    model: String,
    `object`: String,
    created: Long,
    @fieldName("system_fingerprint") systemFingerprint: Option[String],
    choices: List[Choice],
    usage: Option[Usage],
  ) extends CompletionsResponse

  final case object StreamDone extends CompletionsResponse

  implicit val schema: Schema[CompletionsResponse] = DeriveSchema.gen[CompletionsResponse]
  implicit val completionsResponseJsonCodec: zio.json.JsonCodec[CompletionsResponse] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)

  private def preDeserializeFix(raw: String): String =
    Option
      .when(raw.contains("\"chat.completion.chunk\""))(s"""{"Chunk":$raw}""")
      .getOrElse(raw)

  val fromServerSideEvent: ServerSentEvent[String] => Either[String, CompletionsResponse] = _.data match {
    case s if s.trim == "[DONE]" => Right(StreamDone)
    case other                   => completionsResponseJsonCodec.decodeJson(preDeserializeFix(other))
  }
}

final case class Choice(
  index: Long,
  text: Option[String],
  delta: Option[Delta],
  @fieldName("finish_reason") finishReason: Option[String],
  @fieldName("native_finish_reason") nativeFinishReason: Option[String],
  // TODO: Handle "logprobs"
  // logprobs: Option[String]
)

final case class Delta(
  role: String,
  content: String,
  // TODO: Add "reasoning"
  // TODO: Add "reasoning_details"
)
final case class Usage(
  @fieldName("prompt_tokens") promptTokens: Long,
  @fieldName("completion_tokens") completionTokens: Long,
  @fieldName("total_tokens") totalTokens: Long,
  cost: Option[Double],
  @fieldName("is_byok") isBYOK: Option[Boolean],
  // TODO: Add others.
)
