package zio.llm.openrouter

import zio.Chunk
import zio.json.JsonCodec
import zio.llm.openrouter.Completions._
import zio.llm.{Model, Prompt}
import zio.schema.annotation.fieldName
import zio.schema.codec.{JsonCodec => SchemaJsonCodec}
import zio.schema.{DeriveSchema, Schema}

private[openrouter] final case class CompletionsRequest(
  model: Model,
  messages: Seq[Message] = Seq.empty,
  prompt: Option[Prompt] = None,
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
  implicit val seqMessageSchema: Schema[Seq[Message]] = Schema.chunk[Message].transform(_.toSeq, Chunk.fromIterable(_))

  private implicit val schema: Schema[CompletionsRequest]                 = DeriveSchema.gen
  implicit val completionsRequestJsonCodec: JsonCodec[CompletionsRequest] = SchemaJsonCodec.jsonCodec(schema)
}
