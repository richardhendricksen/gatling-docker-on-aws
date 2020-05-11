package nl.codecontrol.gatling.simulations

import io.gatling.core.Predef._
import nl.codecontrol.gatling.scenarios.BasicScenario.basicScenario
import nl.codecontrol.gatling.config.Config._

import scala.language.postfixOps

class BasicSimulation extends Simulation {

  println("Configuration:")
  println("BaseURL: " + baseUrl)
  println("Nr concurrent users: " + users)
  println("Max duration: " + maxDuration)
  println("RampUp time: " + rampUpTime)

  setUp(
    basicScenario.inject(rampUsers(users) during rampUpTime)).maxDuration(maxDuration).protocols(httpProtocol)
    .assertions(
      global.failedRequests.count.is(0)
    )
}
