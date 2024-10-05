package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.UserData
import org.mindrot.jbcrypt.BCrypt
import queries.GetSqlQueries

@Singleton
class LoginService @Inject() (mariadbQueries: GetSqlQueries)(
    implicit ec: ExecutionContext
) {
  def getUserData(username: String, password: String): Future[Option[UserData]] = {
    mariadbQueries.getUserData(username).map { resultOption =>
      resultOption.flatMap { result =>
        val fixedPassword = result.hashedPassword.replace("$2y$", "$2a$")
        if (BCrypt.checkpw(password, fixedPassword)) {
          Some(result)
        } else {
          None
        }
      }
    }
  }
}
