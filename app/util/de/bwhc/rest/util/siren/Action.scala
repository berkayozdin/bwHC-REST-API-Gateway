package de.bwhc.rest.util.siren


import play.api.libs.json.Json

import shapeless.{
  HList, HNil, ::, Lazy
}


object Method extends Enumeration
{
  type Method = Value

  val DELETE, GET, PATCH, POST, PUT = Value  

  implicit val format = Json.formatEnum(this)
}


//case class Action[Fs <: HList]
case class Action
(
  name: String,
  method: Method.Value,
  href: String,
//  name: Option[String] = None,
  title: Option[String] = None,
  `type`: Option[MediaType] = None
)
{

//  def withName(n: String) =
//    copy(name = Some(n))

  def withTitle(t: String) =
    copy(title = Some(t))

  def withType(mime: MediaType) =
    copy(`type` = Some(mime))

  def withMediaType(mime: String) =
    withType(MediaType(mime))

}


object Action
{

  def apply(
    name: String,
    method: Method.Value,
    href: String
  ): Action =
    Action(name,method,href, None, None)

  import scala.language.implicitConversions

//  implicit def fromPair(p: (Method.Value,String)) =
//    Action(p._1,p._2)
     
  implicit def syntax3(p: (String,(Method.Value,String))) =
    Action(p._1,p._2._1,p._2._2)



  implicit val format = Json.format[Action] 

}
