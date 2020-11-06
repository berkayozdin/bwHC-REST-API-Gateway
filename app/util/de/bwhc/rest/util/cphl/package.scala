package de.bwhc.rest.util


import java.net.URI

import play.api.libs.json
import play.api.libs.json._


package object cphl {

/*
 * A few experimental utilities to provide hypermedia support based on CPHL:
 *
 * https://github.com/mikestowe/CPHL
 *
 */

object Method extends Enumeration
{
  type Method = Value

  val GET, POST, PUT, PATCH, DELETE = Value

  implicit val format = Json.formatEnum(this)
      
}



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

  val Self     = Relation("self")
  val Create   = Relation("create")
  val Read     = Relation("read")
  val Update   = Relation("update")
  val Delete   = Relation("delete")
  val Search   = Relation("search")
  val First    = Relation("first")
  val Last     = Relation("last")
  val Prev     = Relation("prev")
  val Next     = Relation("next")
  val Base     = Relation("base")

}


case class Action
(
  title: Option[String] = None,
  description: Option[String] = None,
  href: URI,
  methods: Set[Method.Value],
  formats: Map[Action.Format.Name,Action.Format] = Map.empty[Action.Format.Name,Action.Format]
/*
  TODO
  rel: Relation,
  expiration: Instant
*/  
)
{
  def withFormats(fs: (Action.Format.Name,Action.Format)*): Action =
    this.copy(formats = formats ++ fs.toMap)
}

object Action
{

  def apply(
    href: URI,
    methods: Set[Method.Value]
  ): Action =
    Action(None,None,href,methods)


  def apply(
    href: String,
    methods: Method.Value*
  ): Action =
    Action(None,None,URI.create(href),methods.toSet)

  def apply(
    title: String,
    description: String,
    href: String,
    method: Method.Value,
    formats: (Action.Format.Name,Action.Format)*
  ): Action = 
    Action(Some(title),Some(description),URI.create(href),Set(method),formats.toMap)


  final case class Format(
    mimeType: Option[String],
    schema: Option[URI]
  )

  object Format
  {

    case class Name(name: String) extends AnyVal

    val JSON = Name("json")
    val XML  = Name("xml")

    def apply(mimeType: String, schema: String): Format =
      Format(Some(mimeType), Some(URI.create(schema)))

    def apply(mimeType: String): Format =
      Format(Some(mimeType), None)


    implicit val formatName = Json.valueFormat[Name]

    implicit val format = Json.format[Format]
  }

  implicit val formatFormats: json.Format[Map[Action.Format.Name,Action.Format]] =
    json.Format(
      Reads(js =>
        js.validate[List[(Action.Format.Name,Action.Format)]]
          .map(_.toMap)
      ),
      Writes( fs =>
        JsObject(fs.map { case (n,f) => (n.name -> Json.toJson(f)) })
          
      )
    )

  implicit val format = Json.format[Action]

}


case class CPHL[T <: Product]
(
  state: Option[T],
  links: Map[Relation,Action]
)
{
  def withLinks(newLinks: (Relation,Action)*): CPHL[T] =
    CPHL(state, links ++ newLinks.toMap)
}

object CPHL
{

  def apply[T <: Product](t: T, links: (Relation,Action)*): CPHL[T] =
    CPHL(Some(t),links.toMap)

  def apply[T <: Product](t: Option[T], links: (Relation,Action)*): CPHL[T] =
    CPHL(t,links.toMap)

  def empty[T <: Product](links: (Relation,Action)*): CPHL[T] =
    CPHL(None,links.toMap)


  implicit def format[T <: Product: Format]: Format[CPHL[T]] = {
    Format(
      Reads {
        js =>
          for {
            state   <- js.validateOpt[T]
            actions <- (js \ "_links").validate[List[(String,Action)]]
                         .map(_.map{ case (rel,link) => (Relation(rel) -> link) }.toMap)
          } yield CPHL(state,actions)

      },
      Writes(
        cphl =>
          cphl.state.map(Json.toJson(_).as[JsObject]).getOrElse(JsObject.empty) +
          ("_links" -> JsObject(cphl.links.map { case (rel,link) => (rel.toString -> Json.toJson(link)) }))
      )
    )
  }


}


object syntax
{

  implicit class CPHLSyntax[T <: Product](val t: T) extends AnyVal
  {

    def withActions(links: (Relation,Action)*): CPHL[T] =
      CPHL(t, links: _*)

    def withHypermedia(implicit hyper: T => CPHL[T]): CPHL[T] =
      hyper(t)
  }
}


}
