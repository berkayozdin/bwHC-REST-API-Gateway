package de.bwhc.rest.util.siren


import play.api.libs.json._

import shapeless.{
  HList, HNil, ::, Lazy
}


sealed trait SubEntity[T]
{
  val `class`: Option[String]
  val rel: String
}

final case class EntityLink[T]
(
  `class`: Option[String],
  rel: String,
  href: String
)
extends SubEntity[T]
{
  
}

object EntityLink
{
  implicit def format[T] = Json.format[EntityLink[T]]
}



final case class EmbeddedEntity[T <: Product]
(
  `class`: Option[String],
  rel: String,
  properties: Option[T],
  links: List[Link] = List.empty[Link],
  actions: List[Action] = List.empty[Action]
)
extends SubEntity[T]
{

  def withLinks(ls: Link*): EmbeddedEntity[T] =
    copy(links = links ++ ls)

  def withActions(as: Action*): EmbeddedEntity[T] =
    copy(actions = actions ++ as)

}

object EmbeddedEntity
{
  implicit def format[T <: Product: Format] = Json.format[EmbeddedEntity[T]]
}


object SubEntity
{
  implicit def format[T <: Product](
    implicit
    lf: Format[EntityLink[T]],
    ef: Format[EmbeddedEntity[T]]
  ): Format[SubEntity[T]] =
    Format(
      Reads(
        js =>
          for {
            href   <- (js \ "hrel").validateOpt[String]
            result <- href.map(r => js.validate[EntityLink[T]])
                        .getOrElse(js.validate[EmbeddedEntity[T]])
          } yield result
      ),
      Writes {
        case link: EntityLink[T]       => Json.toJson(link)
        case entity: EmbeddedEntity[T] => Json.toJson(entity)
      }
    )
}



case class Entity[T <: Product, Es <: HList]
(
  `class`: Option[String],
  properties: Option[T],
  entities: Es,
  links: List[Link] = List.empty[Link],
  actions: List[Action] = List.empty[Action]
)
{

  def withEntity[U](entity: SubEntity[U]): Entity[T, SubEntity[U] :: Es] =
    copy(entities = entity :: entities)

  def withLinks(ls: Link*): Entity[T, Es] =
    copy(links = links ++ ls)

  def withActions(as: Action*): Entity[T, Es] =
    copy(actions = actions ++ as)

}


object Entity 
{

//  sealed trait ClassOf[T <: Product]{ val values: Array[String] }
  sealed trait ClassOf[T <: Product]{ val value: String }

  object ClassOf
  {
//    def apply[T <: Product](cl: String): ClassOf[T] = new ClassOf[T]{ val values = Array(cl) }
    def apply[T <: Product](cl: String): ClassOf[T] = new ClassOf[T]{ val value = cl }

    def apply[T <: Product](implicit cl: ClassOf[T]) = cl
  }


  def apply[T <: Product](t: T): Entity[T, HNil] =
    Entity[T, HNil](None, Some(t), HNil)
  
  def apply[T <: Product](t: T, cl: String): Entity[T, HNil] =
    Entity[T, HNil](Some(cl), Some(t), HNil)
  
  val empty: Entity[JsObject, HNil] =
    Entity[JsObject, HNil](None,None, HNil)


  implicit def formatHNil: Format[HNil] =
    Format(
      Reads(js => JsSuccess(HNil)),
      Writes(hnil => JsArray.empty)
    )

  implicit def formatHList[H, T <: HList](
    implicit
    fh: Lazy[Format[H]],
    ft: Format[T]
  ): Format[H :: T] =
    Format(
      Reads(
        js =>
          for {
            arr <- js.validate[JsArray]
            h   <- arr.value.map(fh.value.reads)
                       .find(_.isSuccess)
                       .getOrElse(JsError(s"No valid Entry found in ${arr}"))
            t   <- ft.reads(js)
          } yield h :: t
      ),
      Writes {
        case h :: t    => Json.arr(fh.value.writes(h)) ++ ft.writes(t).as[JsArray] 
      }
    )


  implicit def format[T <: Product: Format, Es <: HList: Format] = Json.format[Entity[T,Es]]

}


object syntax
{

  implicit class SirenOps[T <: Product](val t: T) extends AnyVal
  {

    def toEntity = Entity(t)

  }


}


