package de.bwhc.rest.util.siren


import shapeless.{
  HList, HNil, ::, Lazy
}

import play.api.libs.json._


object json
{

  implicit def formatHNil: Format[HNil] =
    Format(
      Reads(js => JsSuccess(HNil)),
      Writes(hnil => JsArray.empty)
    )


  import scala.collection.{BuildFrom,Factory}


  implicit def formatHListIterableHead[H, C[X] <: Iterable[X], T <: HList](
    implicit
    fh: Lazy[Format[H]],
    ft: Format[T],
    fac: Factory[H,C[H]],
    bf: BuildFrom[C[H],H,C[H]]
  ): Format[C[H] :: T] =
    Format(
      Reads(
        js =>
          for {
            arr <- js.validate[JsArray]
            hs  <- JsSuccess(
                     arr.value
                       .map(fh.value.reads)
                       .filter(_.isSuccess)
                       .map(_.get)
                       .to(fac)
                   )
            t   <- ft.reads(js)
          } yield hs :: t
      ),
      Writes {
        case hs :: t => new JsArray(hs.map(fh.value.writes).toIndexedSeq) ++ ft.writes(t).as[JsArray] 
      }
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
}


