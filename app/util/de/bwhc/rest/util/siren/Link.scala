package de.bwhc.rest.util.siren


import play.api.libs.json.Json


case class Link
(
//  rel: Array[String],
  rel: String,
  href: String,
  title: Option[String] = None,
  `type`: Option[MediaType] = None
)
{
  def withTitle(ttl: String) =
    copy(title = Some(ttl))

  def withType(mime: MediaType) =
    copy(`type` = Some(mime))

  def withMediaType(mime: String) =
    withType(MediaType(mime))
}


object Link
{

  import scala.language.implicitConversions

  implicit def fromPair(
    l: (String,String)
  ): Link =
    Link(l._1,l._2)



  implicit val format = Json.format[Link]

}

