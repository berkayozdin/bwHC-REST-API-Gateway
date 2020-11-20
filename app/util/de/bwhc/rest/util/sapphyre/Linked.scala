package de.bwhc.rest.util.sapphyre



case class Link
(
  href: String
)


trait Linked
{

  type Links = Map[String,Link]

  val links: Links

}
