package commercial

import common.{AkkaAgent, Logging}

sealed trait FeedAction {}

object FeedAction {
  case object Fetch extends FeedAction { override def toString = "fetch" }
  case object Parse extends FeedAction { override def toString = "parse" }
}

private[commercial] case class CommercialFeedEvent(feedName: String,
                                                   feedAction: FeedAction,
                                                   success: Boolean){
  val actionAndResult: String = s"$feedAction-${if (success) "success" else "failure"}"
}

private object CommercialLifecycleMetrics extends Logging {

  val feedEvents = AkkaAgent[Seq[CommercialFeedEvent]](Seq.empty)

  private[commercial] def logEvent(event: CommercialFeedEvent) = feedEvents.send(_ :+ event)

  def updateMetrics(): Unit = {

    def aggregate(key: String, events: Seq[CommercialFeedEvent]) = key -> events.size.toDouble

    val metricsByActionAndResult = feedEvents.get groupBy (_.actionAndResult) map Function.tupled(aggregate _)
    log.info(s"Updating commercial feed metrics: $metricsByActionAndResult")
    CommercialMetrics.metrics.put(metricsByActionAndResult)
    CommercialMetrics.metrics.upload()
    feedEvents.send(Seq.empty)
  }
}
