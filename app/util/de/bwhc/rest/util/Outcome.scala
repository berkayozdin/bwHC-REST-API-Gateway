package de.bwhc.rest.util


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

    implicit val writes = Json.writes[Issue]

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
