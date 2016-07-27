package abacus

import com.gu.abacus.models._
import common.{AkkaAgent, AkkaAsync, ExecutionContexts, JobScheduler, Jobs, LifecycleComponent, Logging}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import services.AbacusApi

import scala.concurrent.{ExecutionContext, Future}

object AbacusAgent extends Logging with ExecutionContexts {
  private val agent = AkkaAgent[Seq[Mvt]](Nil)

  def update() {
    log.info("Refreshing Abacus tests")
    AbacusApi.testsFor(Web).map(_.map(agent.send))
  }

  def tests: Seq[Mvt] = agent.get

  def javascriptConfig: String = Json.toJson(tests.map(mvt => Json.obj(
    "name" -> mvt.name,
    "audience" -> mvt.audience,
    "offset" -> mvt.offset,
    "variants" -> mvt.variants.map(_.id)
  ))).toString
}


class AbacusAgentLifecycle(
  appLifecycle: ApplicationLifecycle,
  jobs: JobScheduler = Jobs,
  akkaAsync: AkkaAsync = AkkaAsync)(implicit ec: ExecutionContext) extends LifecycleComponent {

  appLifecycle.addStopHook { () => Future {
    jobs.deschedule("AbacusAgentRefreshJob")
  }}

  override def start() = {
    jobs.deschedule("AbacusAgentRefreshJob")

    // update every 5 min
    jobs.schedule("AbacusAgentRefreshJob", "0 5 * * * ?") {
      AbacusAgent.update
    }

    akkaAsync.after1s(AbacusAgent.update)
  }
}
