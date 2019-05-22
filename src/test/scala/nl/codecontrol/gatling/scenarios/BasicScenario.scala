package nl.codecontrol.gatling.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object BasicScenario extends Simulation {

  val basicScenario = scenario("BasicSimulation")
    .exec(http("request_1")
    .get("/"))
    .pause(5)
}
