package nl.codecontrol.gatling.scenarios

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._

object BasicScenario extends Simulation {

  val basicScenario: ScenarioBuilder = scenario("BasicSimulation")
    .doWhile(true, "mainLoop") {
      exec(http("request_1")
        .get("/"))
        .pause(5)
    }

}
