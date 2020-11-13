package de.bwhc.rest.util.siren


import play.api.libs.json._

import shapeless.{
  HList, HNil, ::
}


sealed trait SubEntity[T,Es <: HList]
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
extends SubEntity[T,HNil]

object EntityLink
{

  def apply[T](
    rel: String,
    href: String
  ): EntityLink[T] =
    EntityLink(None,rel,href)

  implicit def format[T] = Json.format[EntityLink[T]]
  
}


final case class EmbeddedEntity[T <: Product, Es <: HList]
(
  `class`: Option[String],
  rel: String,
  properties: Option[T],
  entities: Es,
  links: List[Link] = List.empty[Link],
  actions: List[Action] = List.empty[Action]
)
extends SubEntity[T,Es]
{

  def withLinks(ls: Link*) =
    copy(links = links ++ ls)

  def withActions(as: Action*) =
    copy(actions = actions ++ as)

  def withEntity[U, Es <: HList](entity: SubEntity[U,Es]) =
    copy(entities = entity :: entities)

  def withEntities[U, Es <: HList](es: Seq[SubEntity[U,Es]]) =
    copy(entities = es :: entities)

}

object EmbeddedEntity
{

  def apply[T <: Product](t: T, rel: String): EmbeddedEntity[T, HNil] =
    EmbeddedEntity[T, HNil](None, rel, Some(t), HNil)
  
  def apply[T <: Product](cl: String, t: T, rel: String): EmbeddedEntity[T, HNil] =
    EmbeddedEntity[T, HNil](Some(cl), rel, Some(t), HNil)
  
  def apply(rel: String): EmbeddedEntity[JsObject, HNil] =
    EmbeddedEntity[JsObject, HNil](None,rel,None, HNil)

  implicit def format[T <: Product: Format, Es <: HList: Format] =
    Json.format[EmbeddedEntity[T,Es]]

}


object SubEntity
{

  implicit def format[T <: Product, Es <: HList](
    implicit
    lf: Format[EntityLink[T]],
    ef: Format[EmbeddedEntity[T,Es]]
  ): Format[SubEntity[T,Es]] =
    Format(
      Reads(
        js =>
          for {
            href   <- (js \ "hrel").validateOpt[String]
            result <- href.map(r => js.validate[EntityLink[T]])
                        .getOrElse(js.validate[EmbeddedEntity[T,Es]])
                        .map(_.asInstanceOf[SubEntity[T,Es]])
          } yield result
      ),
      Writes {
        case link:   EntityLink[T]     => Json.toJson(link)
        case entity: EmbeddedEntity[T,Es] => Json.toJson(entity)
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

  def withClass(cl: String) =
    copy(`class` = Some(cl))

  def withEntity[U, Es <: HList](entity: SubEntity[U,Es]) =
    copy(entities = entity :: entities)

  def withEntities[U, Es <: HList](es: Seq[SubEntity[U,Es]]) =
    copy(entities = es :: entities)

  def withLinks(ls: Link*): Entity[T, Es] =
    copy(links = links ++ ls)

  def withNewLinks(ls: Link*): Entity[T, Es] =
    copy(links = ls.toList)

  def withProperties[U <: Product](ps: U): Entity[U, Es] =
    copy(properties = Some(ps))

  def withActions(as: Action*): Entity[T, Es] =
    copy(actions = actions ++ as)

  def withNewActions(as: Action*): Entity[T, Es] =
    copy(actions = as.toList)

}


object Entity 
{

  sealed trait ClassOf[T <: Product]{ val value: String }

  object ClassOf
  {
    def apply[T <: Product](cl: String): ClassOf[T] = new ClassOf[T]{ val value = cl }

    def apply[T <: Product](implicit cl: ClassOf[T]) = cl
  }


  def apply[T <: Product](t: T): Entity[T, HNil] =
    Entity[T, HNil](None, Some(t), HNil)
  
  def apply[T <: Product](t: T, cl: String): Entity[T, HNil] =
    Entity[T, HNil](Some(cl), Some(t), HNil)
  
  val empty: Entity[JsObject, HNil] =
    Entity[JsObject, HNil](None,None, HNil)


  def withClass(cl: String) =
    empty.withClass(cl)

  def withEntity[U, Es <: HList](entity: SubEntity[U,Es]) =
    empty.withEntity(entity)

  def withEntities[U, Es <: HList](es: Seq[SubEntity[U,Es]]) =
    empty.withEntities(es)

  def withLinks(ls: Link*): Entity[JsObject, HNil] =
    empty.withLinks(ls: _*)

  def withActions(as: Action*): Entity[JsObject,HNil] =
    empty.withActions(as: _*)


  implicit def format[T <: Product: Format, Es <: HList: Format] = Json.format[Entity[T,Es]]

}


object syntax
{

  implicit class SirenOps[T <: Product](val t: T) extends AnyVal
  {

    def toEntity = Entity(t)

    def toEmbedded(rel: String) = EmbeddedEntity(t,rel)

  }


}
