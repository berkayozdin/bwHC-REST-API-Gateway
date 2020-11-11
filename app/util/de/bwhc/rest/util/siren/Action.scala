package de.bwhc.rest.util.siren


import play.api.libs.json.Json


object Method extends Enumeration
{
  type Method = Value

  val DELETE, GET, PATCH, POST, PUT = Value  

  implicit val format = Json.formatEnum(this)
}


case class Action
(
  method: Method.Value,
  href: String,
  name: Option[String] = None,
  title: Option[String] = None,
  `type`: Option[MediaType] = None
)
{

  def withName(n: String) =
    copy(name = Some(n))

  def withTitle(t: String) =
    copy(title = Some(t))

  def withType(mime: MediaType) =
    copy(`type` = Some(mime))

  def withMediaType(mime: String) =
    withType(MediaType(mime))

}


object Action
{

  implicit val format = Json.format[Action] 

}
