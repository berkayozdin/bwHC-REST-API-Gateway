package de.bwhc.rest.util.scapphyre


import shapeless.{
  HList, HNil, ::
}

import play.api.libs.json.{
  JsObject, Json
}

import cats.data.NonEmptyList



sealed trait Document extends Linked


case class Resource[T, Meta, Embedded <: HList]
(
  data: T,
  meta: Option[Meta] = None,
  links: Map[String,Link] = Map.empty[String,Link],
  actions: Map[String,Action] = Map.empty[String,Action],
  embedded: Embedded
)
extends Document
{

  def withData[U](u: U) =
    copy(data = u)

  def withMeta[M](m: M) =
    copy(meta = Some(m))

  def withLinks(ls: (String,Link)*) =
    copy(links = links ++ ls)

  def withActions(as: (String,Action)*) =
    copy(actions = actions ++ as)

  def withEmbedded[R](rel: String, r: R)(implicit emb: R IsIn Embeddable) =
    copy(embedded = (rel -> r) :: embedded)

}




object Resource
{

  def apply[T](t: T): Resource[T,JsObject,HNil] =
    Resource(data = t, embedded = HNil)

  val empty: Resource[JsObject,JsObject,HNil] =
    Resource(data = JsObject.empty, embedded = HNil)

}


object Collection
{

  case class Properties private (total: Int)

  object Properties
  {
    def apply[T, C[X] <: Iterable[X]](ts: C[T]): Properties =
      Properties(ts.size)

    implicit val format = Json.format[Properties]  

  }


  def apply[T, C[X] <: Iterable[X]](
    rel: String, ts: C[T]
  )(
    implicit emb: C[T] IsIn Embeddable
  ): Resource[Collection.Properties, JsObject, (String,C[T]) :: HNil] =
    Resource(Properties(ts))
      .withEmbedded(rel,ts)


  def apply[T, C[X] <: Iterable[X]](
    ts: C[T]
  )(
    implicit emb: C[T] IsIn Embeddable
  ): Resource[Collection.Properties, JsObject, (String,C[T]) :: HNil] =
    apply("items",ts)


}



case class ErrorReport[E]
(
  errors: NonEmptyList[E],
  links: Map[String,Link] = Map.empty[String,Link]
)
extends Document

