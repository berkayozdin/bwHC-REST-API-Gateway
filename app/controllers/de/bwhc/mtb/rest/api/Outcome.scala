package de.bwhc.mtb.rest.api


import play.api.libs.json.{
  Json,
  JsValue,
  JsPath,
  JsonValidationError,
  Writes
}



final case class Outcome
(
  issues: Iterable[Outcome.Issue]
)


object Outcome
{

  final case class Issue
  (
    severity: Issue.Severity.Value,
    details: String
  )

  object Issue
  {

    object Severity extends Enumeration
    {
      val Fatal       = Value("fatal")
      val Error       = Value("error")
      val Warning     = Value("warning")
      val Information = Value("information")

      implicit val format = Json.formatEnum(this)
    }


    def error(details: String)   = Issue(Severity.Error,details)

    def warning(details: String) = Issue(Severity.Warning,details)


    implicit def writes[I <: Issue]: Writes[I] =
      Writes {
        case Issue(severity,details) =>
          Json.obj(
            "severity" -> severity,
            "details"  -> details
          )       
      }

  }


  implicit def writes: Writes[Outcome] = Json.writes[Outcome]


  def fromJsErrors(
    errors: Iterable[(JsPath, Iterable[JsonValidationError])]
  ): Outcome = {

    Outcome(
      errors.flatMap {
        case (path,errs) =>
          errs.map(e => Issue.error(s"${path.toString}: ${e.message}"))
      }
    )

  }

  def fromErrors(
    errors: Iterable[String]
  ): Outcome = {
    Outcome(errors.map(Issue.error))
  }



}
