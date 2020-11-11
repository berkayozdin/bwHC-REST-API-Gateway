package de.bwhc.rest.util.siren


import play.api.libs.json.Json


case class Link
(
  rel: String,
//  rel: Array[String],
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
/*
  def apply(
    rel: String,
    href: String
  ): Link =
    Link(Array(rel), href)
*/

  implicit val format = Json.format[Link]

}

