package de.bwhc.rest.util.sapphyre


import play.api.libs.json._

import shapeless.{
  HList, HNil, ::, Lazy
}

import cats.data.NonEmptyList


object playjson
{


  implicit def writesNel[T: Writes](
    implicit
    writes: Writes[List[T]]
  ): Writes[NonEmptyList[T]] =
    writes.contramap(_.toList)
    


  implicit def writesHNil: Writes[HNil] =
    Writes(hnil => JsObject.empty)


  implicit def writesHList[H, T <: HList](
    implicit
    fh: Lazy[Writes[H]],
    ft: Writes[T]
  ): Writes[(String,H) :: T] =
    Writes {
      case (k,h) :: t  => Json.obj(k -> fh.value.writes(h)) ++ ft.writes(t).as[JsObject]
    }


  implicit def writesMap[T: Writes]: Writes[Map[String,T]] =
    Writes(
      m => new JsObject(m.view.mapValues(Json.toJson(_)).toMap)
    )


  implicit def writesSymbol[S <: Symbol]: Writes[S] =
    Writes(sym => JsString(sym.name))


  implicit val writesMethod =
    Json.formatEnum(Method)


  implicit val writesLink =
    Json.writes[Link]


  implicit val writesMediaType =
    Json.valueFormat[MediaType]


  implicit val writesAction =
    Json.writes[Action]


  implicit val writesCollProp =
    Json.writes[Collection.Properties]



  import de.bwhc.util.syntax.piping._


  implicit def writesResource[
    T: Writes,
    Meta: Writes, 
    Es <: HList: Writes
  ]: Writes[Resource[T,Meta,Es]] =
    Writes {
      case Resource(data,meta,links,actions,embedded) =>
 
        Json.toJson(data).as[JsObject] | 
          (js => embedded match {
                   case HNil => js
                   case _    => js + ("_embedded" -> Json.toJson(embedded))
                 }) |
          (js => meta.fold              (js)(m => js + ("_meta"    -> Json.toJson(m)))) |
          (js => links.headOption.fold  (js)(h => js + ("_links"   -> Json.toJson(links)))) |
          (js => actions.headOption.fold(js)(h => js + ("_actions" -> Json.toJson(actions)))) 
    }


  implicit def writesErrorReport[E: Writes] =
    Json.writes[ErrorReport[E]]








}
