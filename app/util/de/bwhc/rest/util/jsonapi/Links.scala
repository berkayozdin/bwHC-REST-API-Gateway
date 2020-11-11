package de.bwhc.rest.util.jsonapi


sealed trait Link

case class URI(link: String) extends Link

case class HRef(link: String) extends Link

case class MetaHRef[T](link: String, meta: T) extends Link
