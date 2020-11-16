package de.bwhc.rest.util.scapphyre


import play.api.libs.json._

import shapeless.{
  HList, HNil, ::, Lazy
}

import cats.data.NonEmptyList


object playjson
{


  implicit def formatNel[T: Reads: Writes](
    implicit
    reads: Reads[List[T]],
    writes: Writes[List[T]]
  ): Format[NonEmptyList[T]] =
    Format[NonEmptyList[T]](
      reads
        .filterNot(JsonValidationError("Found empty list where non-empty list expected"))(_.isEmpty)
        .map(NonEmptyList.fromListUnsafe),
      writes.contramap(_.toList)
    )


  implicit def formatHNil: Format[HNil] =
    Format(
      Reads(js => JsSuccess(HNil)),
      Writes(hnil => JsObject.empty)
    )


  implicit def formatHList[H, T <: HList](
    implicit
    fh: Lazy[Format[H]],
    ft: Format[T]
  ): Format[(String,H) :: T] =
    Format(
      Reads(
        js => ??? //TODO
      ),
      Writes {
        case (k,h) :: t  => Json.obj(k -> fh.value.writes(h)) ++ ft.writes(t).as[JsObject]
      }
    )


  implicit def formatMap[T: Format]: Format[Map[String,T]] =
    Format(
      Reads(
        js => ???   //TODO
      ),
      Writes(
        m => new JsObject(m.view.mapValues(Json.toJson(_)).toMap)
      )
    )



  implicit val formatMethod =
   Json.formatEnum(Method)


  implicit val formatLink =
    Json.format[Link]


  implicit val formatMediaType =
    Json.valueFormat[MediaType]


  implicit val formatAction =
    Json.format[Action]




  implicit class ChainingSyntax[T](val t: T) extends AnyVal 
  {
    def |[U](f: T => U) = f(t)
  }



  implicit def formatResource[
    T: Format,
    Meta: Format, 
    Es <: HList: Format
  ]: Format[Resource[T,Meta,Es]] =
    Format(
      Reads(js => ???),
      Writes {
        case Resource(data,meta,links,actions,embedded) =>

          (Json.toJson(data).as[JsObject] +
            ("_embedded" -> Json.toJson(embedded)) ) |
            (js => if (!meta.isEmpty)    js + ("_meta"    -> Json.toJson(meta))    else js ) |
            (js => if (!links.isEmpty)   js + ("_links"   -> Json.toJson(links))   else js ) |
            (js => if (!actions.isEmpty) js + ("_actions" -> Json.toJson(actions)) else js ) 

/*
          Json.toJson(data).as[JsObject] +
            ("_meta"     -> Json.toJson(meta))    +
            ("_links"    -> Json.toJson(links))   +
            ("_actions"  -> Json.toJson(actions)) +
            ("_embedded" -> Json.toJson(embedded))
*/
      }
    )


  implicit def formatErrorReport[E: Format] =
    Json.format[ErrorReport[E]]








}
