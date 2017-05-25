package se.kth.app.sim.logoot

import java.util.UUID

import com.typesafe.scalalogging.StrictLogging
import se.kth.app.events._
import se.kth.app.logoot.{Insert, Operation, Remove}
import se.kth.app.ports.AppPort
import se.kth.app.sim.{SimulationResultMap, SimulationResultSingleton}
import se.sics.kompics.Start
import se.sics.kompics.sl.{ComponentDefinition, Init, PositivePort, handle}
import se.sics.kompics.timer.Timer
import se.sics.ktoolbox.croupier.CroupierPort
import se.sics.ktoolbox.croupier.event.CroupierSample
import se.sics.ktoolbox.util.network.KAddress

import scala.collection.mutable.ListBuffer

/**
  * Created by reginbald on 21/05/2017.
  */
class LogootTestClient(init: Init[LogootTestClient]) extends ComponentDefinition with StrictLogging  {
  val timer:PositivePort[Timer] = requires[Timer]
  val appPort: PositivePort[AppPort] = requires[AppPort]
  val croupier:PositivePort[CroupierPort]  = requires[CroupierPort]

  private val res:SimulationResultMap = SimulationResultSingleton.getInstance
  //private var timerId: Option[UUID] = None;

  private val (self, simulation) = init match {
    case Init(s: KAddress, sim: Int) => (s, sim)
  }

  var patchCounter:Int = 1
  var patchTotal:Int = 3
  var undo:Int = 0 // todo
  var redo:Int = 0 // todo
  var requestDocOnce:Int = 0

  var patch:se.kth.app.logoot.Patch = _

  var processing: Boolean = false
  var lastPatch: se.kth.app.logoot.Patch = _
  var done: se.kth.app.logoot.Patch = _


  ctrl uponEvent {
    case _: Start => handle {
      logger.info("Starting test client")

      res.put(self.getId + "patch", patchCounter)
      res.put(self.getId + "doc", "")

      //val spt = new ScheduleTimeout(1000);
      //val timeout = new PingTimeout(spt)
      //spt.setTimeoutEvent(timeout)
      //trigger(spt -> timer);
      //timerId = Some(timeout.getTimeoutId());
    }
  }

  appPort uponEvent {
    case AppOut(_:KAddress, Logoot_Doc(doc:String)) => handle {
      logger.info(self + " - Got document")
      res.put(self.getId + "doc", doc)
    }
    case AppOut(_:KAddress, Logoot_Done(_: se.kth.app.logoot.Patch)) => handle {
      logger.info(self + " - logoot processing done")
      done = patch
      if (lastPatch != null && lastPatch.id == done.id) processing = false
    }
    case AppOut(_:KAddress, Logoot_Patch(patch: se.kth.app.logoot.Patch)) => handle {
      logger.info(self + " - done adding patch")
      lastPatch = patch
      if (done != null && lastPatch.id == done.id) processing = false
    }
  }

  //timer uponEvent {
  //  case PingTimeout(_) => handle {
  //    logger.info("Sending Command")
  //    trigger(AppIn(new Ping()) -> appPort)
  //  }
  //}

  croupier uponEvent {
    case _:CroupierSample[_] => handle {
      val tmp = self.getId.toString
      if(!processing) {
        if(patchCounter <= patchTotal){
          if(simulation == 0) insert_simulation()
          if(simulation == 1) remove_simulation()
          if(simulation == 2) undo_simulation()
          if(simulation == 3) redo_simulation()
          patchCounter += 1
        }
      }
      if (patchCounter == patchTotal + 1){
        logger.info("Sending Doc Request Command")
        trigger(AppIn(Logoot_Doc(null)) -> appPort)
      }
    }
  }

  def insert_simulation(): Unit ={
    logger.info("Sending Patch Command")
    processing = true
    patch = se.kth.app.logoot.Patch(UUID.randomUUID(), 0, new ListBuffer[Operation], 3)
    patch.operations += Insert(null, " mom " + patchCounter)
    patch.operations += Insert(null, " dad " + patchCounter)
    patch.operations += Insert(null, " eric " + patchCounter)
    patch.N = 3 // number of insert that need ids
    res.put(self.getId + "patch", patchCounter)
    trigger(AppIn(Logoot_Do(0, patch)) -> appPort)
  }

  def remove_simulation(): Unit ={
    logger.info("Sending Patch Command")
    processing = true
    patch = se.kth.app.logoot.Patch(UUID.randomUUID(), 0, new ListBuffer[Operation], 0)
    if (patchCounter % 2 == 1){
      patch.operations += Insert(null, " mom " + patchCounter)
      patch.operations += Insert(null, " dad " + patchCounter)
      patch.operations += Insert(null, " eric " + patchCounter)
      patch.N = 3 // number of insert that need ids
    } else {
      patch.operations += Remove(lastPatch.operations.head.id, lastPatch.operations.head.content)
    }
    res.put(self.getId + "patch", patchCounter)
    trigger(AppIn(Logoot_Do(0, patch)) -> appPort)
  }

  def undo_simulation(): Unit ={
    logger.info("Sending Patch Command")
    patch = se.kth.app.logoot.Patch(UUID.randomUUID(), 0, new ListBuffer[Operation], 3)
    patch.operations += Insert(null, " mom " + patchCounter)
    patch.operations += Insert(null, " dad " + patchCounter)
    patch.operations += Insert(null, " eric " + patchCounter)
    patch.operations += Remove(null, "eric " + patchCounter)
    res.put(self.getId + "patch", patchCounter)
    trigger(AppIn(Logoot_Do(0, patch)) -> appPort)
    processing = true
  }

  def redo_simulation(): Unit ={
    logger.info("Sending Patch Command")
    processing = true
    res.put(self.getId + "patch", patchCounter)
    if(patchCounter == 1){
      patch = se.kth.app.logoot.Patch(UUID.randomUUID(), 0, new ListBuffer[Operation], 3)
      patch.operations += Insert(null, " mom " + patchCounter)
      patch.operations += Insert(null, " dad " + patchCounter)
      patch.operations += Insert(null, " eric " + patchCounter)
      trigger(AppIn(Logoot_Do(0, patch)) -> appPort)
    } else if(patchCounter == 2){
      patch = se.kth.app.logoot.Patch(UUID.randomUUID(), 0, new ListBuffer[Operation], 0)
      patch.operations += Remove(lastPatch.operations.head.id, lastPatch.operations.head.content)
      trigger(AppIn(Logoot_Do(0, patch)) -> appPort)
    }
    else if(patchCounter == 3){
      trigger(AppIn(Logoot_Undo(lastPatch.id)), appPort)
    } else {

    }
  }

  //override def tearDown(): Unit = {
  //  timerId match {
  //    case Some(id) =>
  //      trigger(new CancelTimeout(id) -> timer)
  //    case None => // nothing
  //  }
  //}

}

