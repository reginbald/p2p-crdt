package se.kth.app.ports

import se.kth.app.events.Logoot_Insert
import se.sics.kompics.sl.Port

class LogootPort extends Port {
  request[Logoot_Insert]
}