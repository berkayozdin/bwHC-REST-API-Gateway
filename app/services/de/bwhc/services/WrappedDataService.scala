package de.bwhc.services


import javax.inject.Singleton


import de.bwhc.rest.util.ServiceWrapper

import de.bwhc.mtb.data.entry.api.MTBDataService


@Singleton
class WrappedDataService extends ServiceWrapper(MTBDataService)



