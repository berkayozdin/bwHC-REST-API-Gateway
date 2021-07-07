package de.bwhc.rest.util.sapphyre




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
  }

  def Fatal(details: String)   = Issue(Severity.Fatal, details)

  def Error(details: String)   = Issue(Severity.Error, details)

  def Warning(details: String) = Issue(Severity.Warning, details)

  def Info(details: String)    = Issue(Severity.Information, details)

}
