package it.unibo.casestudy

import it.unibo.alchemist.model.implementations.positions.Euclidean2DPosition
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.scafi.space.{Point2D, Point3D}
import Services._

class TaskGenerator extends AggregateProgram with StandardSensors with ScafiAlchemistSupport {

  lazy val providedServices: Set[ProvidedService] = {
    if(node.has("providedServices"))
      node.get[Set[ProvidedService]]("providedServices")
    else
      Set.empty
  }

  override def main(): Any = {
    if(!node.has("task")) {
      val task = Task(
        (randomGenerator().shuffle(services) -- providedServices.map(_.service))
          .take(1+randomGenerator().nextInt(3))
      )
      node.put("task", task)
    }
  }
}