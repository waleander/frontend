package feed

import common.{AkkaAsync, Jobs}
import play.api.{ Application => PlayApp, GlobalSettings }

trait OnwardJourneyLifecycle extends GlobalSettings {
  override def onStart(app: PlayApp) {
    super.onStart(app)

    Jobs.deschedule("OnwardJourneyAgentsRefreshJob")
    Jobs.deschedule("OnwardJourneyAgentsRefreshHourlyJob")
    Jobs.deschedule("OnwardJourneyAgentsRefreshDailyJob")

      // fire every min
      Jobs.schedule("OnwardJourneyAgentsRefreshJob", "0 * * * * ?") {
        MostPopularAgent.refresh()
        MostPopularExpandableAgent.refresh()
        GeoMostPopularAgent.refresh()
        MostViewedVideoAgent.refresh()
        MostViewedAudioAgent.refresh()
        MostViewedGalleryAgent.refresh()
      }

      // fire every hour
      Jobs.schedule("OnwardJourneyAgentsRefreshHourlyJob", "0 0 * * * ?") {
        DayMostPopularAgent.refresh()
      }

      // fire every 6 hours
      Jobs.schedule("OnwardJourneyAgentsRefreshDailyJob", "0 0 0/6 * * ?") {
        MostPopularSeriesAgent.refresh()
      }

      AkkaAsync {
        MostPopularAgent.refresh()
        MostPopularExpandableAgent.refresh()
        GeoMostPopularAgent.refresh()
        MostViewedVideoAgent.refresh()
        MostViewedAudioAgent.refresh()
        MostViewedGalleryAgent.refresh()
        DayMostPopularAgent.refresh()
        MostPopularSeriesAgent.refresh()
      }
  }

  override def onStop(app: PlayApp) {
    Jobs.deschedule("OnwardJourneyAgentsRefreshJob")
    Jobs.deschedule("OnwardJourneyAgentsRefreshHourlyJob")
    super.onStop(app)
  }
}
