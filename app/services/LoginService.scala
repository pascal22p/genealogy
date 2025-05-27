package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.OptionT
import models.UserData
import org.mindrot.jbcrypt.BCrypt
import queries.GetSqlQueries

@Singleton
class LoginService @Inject() (mariadbQueries: GetSqlQueries)(
    implicit ec: ExecutionContext
) {
  def getUserData(username: String, password: String): OptionT[Future, UserData] = {
    mariadbQueries.getUserData(username).transform {
      case Some(result) =>
        val fixedPassword = result.hashedPassword.replace("$2y$", "$2a$")
        if (BCrypt.checkpw(password, fixedPassword)) {
          Some(result)
        } else {
          None
        }
      case _ => None
    }
  }
}
