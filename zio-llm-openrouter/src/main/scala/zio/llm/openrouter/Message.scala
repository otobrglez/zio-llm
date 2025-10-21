package zio.llm.openrouter

import zio.schema.annotation.{caseName, discriminatorName}
import zio.schema.{DeriveSchema, Schema}

@discriminatorName("role")
sealed trait Message {
  def content: String
}

object Message {
  implicit val schema: Schema[Message]                       = DeriveSchema.gen[Message]
  implicit val messageJsonCodec: zio.json.JsonCodec[Message] = zio.schema.codec.JsonCodec.jsonCodec(schema)

  def system(content: String): SystemMessage       = SystemMessage(content = content)
  def user(content: String): UserMessage           = UserMessage(content = content)
  def developer(content: String): DeveloperMessage = DeveloperMessage(content = content)
}

@caseName("system")
final case class SystemMessage(
  content: String,
) extends Message

@caseName("user")
final case class UserMessage(
  content: String,
) extends Message

@caseName("developer")
final case class DeveloperMessage(
  content: String,
) extends Message
