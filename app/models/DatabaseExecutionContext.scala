package models

import javax.inject.*

import org.apache.pekko.actor.ActorSystem
import org.slf4j.MDC
import play.api.libs.concurrent.CustomExecutionContext

@Singleton
class DatabaseExecutionContext @Inject() (system: ActorSystem)
    extends CustomExecutionContext(system, "database.dispatcher") {

  override def execute(runnable: Runnable): Unit = {
    val mdcContext = MDC.getCopyOfContextMap
    super.execute { () =>
      val previous = MDC.getCopyOfContextMap
      if (mdcContext != null) MDC.setContextMap(mdcContext)
      else MDC.clear()
      try runnable.run()
      finally {
        if (previous != null) MDC.setContextMap(previous)
        else MDC.clear()
      }
    }
  }
}
