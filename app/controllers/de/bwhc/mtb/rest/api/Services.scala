package de.bwhc.mtb.rest.api


import javax.inject.Singleton

import de.bwhc.mtb.data.entry.api.MTBDataService
import de.bwhc.mtb.query.api.QueryService


@Singleton
class Services
{

  lazy val dataService = MTBDataService.getInstance.get

  lazy val queryService = QueryService.getInstance.get

}
