package de.bwhc.rest.util.sapphyre



object Method extends Enumeration
{
  type Method = Value

  val DELETE, GET, PATCH, POST, PUT = Value
}


case class Action
(
  method: Method.Value,
  href: String,
  formats: Option[Map[String,Link]] = None
)
{

  def withFormats(fs: (MediaType,Link)*) = {

    val fm =
      fs.toList
        .map { case (MediaType(rel),l) => (rel,l) }
        .toMap

    copy(formats = Some(formats.fold(fm)(_ ++ fm)))

  }

}

object Action
{

  def apply(ml: (Method.Value,String)): Action = 
    Action(ml._1,ml._2)
    
  def apply(m: Method.Value, l: Link): Action = 
    Action(m,l.href)
}
