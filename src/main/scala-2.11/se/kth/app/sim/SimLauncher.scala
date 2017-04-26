package se.kth.app.sim

import se.sics.kompics.simulator.SimulationScenario
import se.sics.kompics.simulator.run.LauncherComp

/**
  * Created by reginbald on 26/04/2017.
  */
object SimLauncher {
  def main(args: Array[String]) {
    SimulationScenario.setSeed(ScenarioSetup.scenarioSeed)
    val simpleBootScenario = ScenarioGen.simpleBoot
    simpleBootScenario.simulate(classOf[LauncherComp])
  }
}
