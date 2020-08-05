package de.bwhc.mtb.rest.api


import javax.inject.Singleton

import de.bwhc.mtb.data.entry.api.MTBDataService


@Singleton
class Services
{

  lazy val dataService = MTBDataService.getInstance.get


}
