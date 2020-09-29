package de.bwhc.rest.auth


import javax.inject.Singleton


import de.bwhc.user.auth.api.UserService


@Singleton
class Services
{

  val userService = UserService.getInstance.get

}
