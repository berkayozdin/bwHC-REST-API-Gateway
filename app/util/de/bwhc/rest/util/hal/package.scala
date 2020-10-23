package de.bwhc.rest.util


import java.net.URI

import play.api.libs.json.{
  Json, JsObject, Format, Reads, Writes
}


package object hal {

/*
 * A few experimental utilities to provide hypermedia support based on HAL:
 *
 * https://en.wikipedia.org/wiki/Hypertext_Application_Language
 *
 */


case class Relation(name: String) extends AnyVal
{
  override def toString = name
}

object Relation
{
  implicit val format = Json.valueFormat[Relation]
}

object Relations
{

  val Self = Relation("self")

  val Previous = Relation("previous")
  val Prev     = Previous

  val Next = Relation("next")

}


case class Link(href: URI)
object Link
{
  implicit val format = Json.format[Link]
}


case class Hyper[T <: Product]
(
  data: T,
  links: Map[Relation,Link]
)

object Hyper
{

  def apply[T <: Product](t: T)(implicit hyper: T => Hyper[T]): Hyper[T] = hyper(t)



  implicit def format[T <: Product: Format]: Format[Hyper[T]] = {
    Format(
      Reads {
        js =>
          for {
            t        <- js.validate[T]
            optLinks <- (js \ "_links").validateOpt[List[(String,Link)]]
            links    =  optLinks.map(_.map{ case (rel,link) => (Relation(rel) -> link) }.toMap)
                         .getOrElse(Map.empty[Relation,Link])           
          } yield Hyper(t,links)

      },
      Writes(
        hyper =>
          Json.toJson(hyper.data).as[JsObject] +
          ("_links" -> JsObject(hyper.links.map { case (rel,link) => (rel.toString -> Json.toJson(link)) }))
      )
    )
  }


}


object syntax
{
  implicit class HyperSyntax[T <: Product](val t: T) extends AnyVal
  {
    def addLinks(links: (Relation,Link)*): Hyper[T] =
      Hyper(t,links.toMap)

    def withLinks(links: (Relation,String)*): Hyper[T] =
      Hyper(t,links.toMap.map { case (rel,href) => (rel -> Link(URI.create(href))) })


    def withHypermedia(implicit hyper: T => Hyper[T]): Hyper[T] = Hyper(t)
  }
}




}
