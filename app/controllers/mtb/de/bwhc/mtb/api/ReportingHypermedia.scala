package de.bwhc.mtb.api


import scala.concurrent.{ExecutionContext,Future}

import de.bwhc.rest.util.sapphyre._

import de.bwhc.auth.api.UserWithRoles

import de.bwhc.util.syntax.piping._



trait ReportingHypermedia
{

  import de.bwhc.rest.util.sapphyre.syntax._
  import de.bwhc.rest.util.sapphyre.Relations._


  private val BASE_URI       = "/bwhc/mtb/api/reporting"

  private val LOCAL_REPORT   = "local-qc-report"
  private val GLOBAL_REPORT  = "global-qc-report"


  private object Scope extends Enumeration
  {
    val Local  = Value("local")
    val Global = Value("global")
  }


  val ApiBaseLink =
    Link(s"$BASE_URI/")


  private def ReportLink(scope: Scope.Value) =
    Link(s"$BASE_URI/QCReport?scope=$scope")


  def ApiResource(
    implicit
    user: UserWithRoles,
    ec: ExecutionContext
  ) = {
    for {

      canGetLocalReport  <- user has QueryPermissions.LocalQCAccessRight

      canGetGlobalReport <- user has QueryPermissions.GlobalQCAccessRight

      api = Resource.empty.withLinks(SELF -> ApiBaseLink)

      result =
        api |
        (r => if (canGetLocalReport)  r.withLinks(LOCAL_REPORT  -> ReportLink(Scope.Local)) else r) |
        (r => if (canGetGlobalReport) r.withLinks(GLOBAL_REPORT -> ReportLink(Scope.Global)) else r)

    } yield result
  }

}
object ReportingHypermedia extends ReportingHypermedia


