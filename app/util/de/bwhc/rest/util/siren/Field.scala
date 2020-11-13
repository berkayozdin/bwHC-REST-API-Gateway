package de.bwhc.rest.util.siren


import play.api.libs.json.Json

import java.net.{URI,URL}
import java.time._




final case class Field[T: Field.TypeName]
(
  name: String,
  `type`: String,
  value: Option[T]
)



object Field
{

  sealed trait TypeName[T]{ val value: String }

  object TypeName
  {

    def apply[T](implicit tn: TypeName[T]) = tn

    private def apply[T](n: String): TypeName[T] =
      new TypeName[T]{ val value = n }


//    implicit val            = TypeName[]("")
    implicit val int           = TypeName[Int]("number")
    implicit val long          = TypeName[Long]("number")
    implicit val float         = TypeName[Float]("number")
    implicit val double        = TypeName[Double]("number")
    implicit val string        = TypeName[String]("text")


    implicit val uri           = TypeName[URI]("url")
    implicit val url           = TypeName[URL]("url")

    implicit val localDate     = TypeName[LocalDate]("dateTime-local")
    implicit val localDateTime = TypeName[LocalDateTime]("dateTime-local")


  }




}

