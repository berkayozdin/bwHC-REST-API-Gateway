package de.bwhc.rest.util


package object jsonapi
{

  type Required[+T] = T

  type Optional[+T] = Option[T]
  



  type Relation = String

  type Links = List[(Relation,Link)]


}
