package se.kth.app.ports

import se.kth.app.events.{BEB_Broadcast, BEB_Deliver}
import se.sics.kompics.sl._

/**
  * Created by reginbald on 28/04/2017.
  */
class BestEffortBroadcast extends Port {
  indication[BEB_Deliver];
  request[BEB_Broadcast];
}
