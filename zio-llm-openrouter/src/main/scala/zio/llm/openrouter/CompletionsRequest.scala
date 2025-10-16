package zio.llm.openrouter

import zio.llm.{Model, Prompt}
import zio.llm.openrouter.Completions._
import zio.schema.{DeriveSchema, Schema}
import zio.schema.annotation.fieldName

private[openrouter] final case class CompletionsRequest(
  model: Model,
  prompt: Prompt,
  models: Option[List[Model]] = None,
  provider: Option[Provider] = None,
  reasoning: Option[Reasoning] = None,
  usage: Option[Usage] = None,
  transforms: Option[List[String]] = None,
  stream: Boolean = false,
  @fieldName("max_tokens") maxTokens: Option[Int] = None,
  temperature: Option[Double] = None,
  seed: Option[Int] = None,
  @fieldName("top_p") topP: Option[Double] = None,
  @fieldName("top_k") topK: Option[Int] = None,
  @fieldName("frequency_penalty") frequencyPenalty: Option[Double] = None,
  @fieldName("repetition_penalty") repetitionPenalty: Option[Double] = None,
  @fieldName("logit_bias") logitBias: Option[Map[String, Double]] = None,
  @fieldName("top_logprobs") topLogProbs: Option[Int] = None,
  @fieldName("min_p") minP: Option[Double] = None,
  @fieldName("top_a") topA: Option[Int] = None,
  user: Option[String] = None,
)

private[openrouter] object CompletionsRequest {
  implicit val schema: Schema[CompletionsRequest] = DeriveSchema.gen[CompletionsRequest]
  implicit val completionsRequestJsonCodec: zio.json.JsonCodec[CompletionsRequest] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)
}
