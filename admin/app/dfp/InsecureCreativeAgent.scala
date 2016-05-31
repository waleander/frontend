package dfp

import common.AkkaAgent
import common.dfp.GuCreative
import conf.switches.Switches.DfpCachingSwitch

import scala.concurrent.Future

object InsecureCreativeAgent {

  private lazy val cache = AkkaAgent(Seq.empty[GuCreative])

  def refresh(): Future[Seq[GuCreative]] = {
    cache alterOff { oldData =>
      if (DfpCachingSwitch.isSwitchedOn) {
        val freshData = DfpApi.readInsecureThirdPartyCreatives()
        if (freshData.nonEmpty) freshData else oldData
      } else oldData
    }
  }

  def get: Seq[GuCreative] = cache.get()
}
