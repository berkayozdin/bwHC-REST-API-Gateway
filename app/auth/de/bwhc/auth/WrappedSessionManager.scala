package de.bwhc.rest.auth


import javax.inject.Singleton


import de.bwhc.rest.util.ServiceWrapper

import de.bwhc.auth.api.UserSessionManager


@Singleton
class WrappedSessionManager extends ServiceWrapper(UserSessionManager)
