package it.unibo.casestudy

import it.unibo.alchemist.model.implementations.positions.Euclidean2DPosition
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.scafi.space.{Point2D, Point3D}

class ServiceDiscovery extends AggregateProgram with StandardSensors with Gradients with BlockG with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with BlockS with BlockC {

  override def currentPosition(): Point3D = {
    val pos = sense[Euclidean2DPosition](LSNS_POSITION)
    Point3D(pos.getX, pos.getY, 0)
  }

  def current2DPosition(): Point2D = Point2D(currentPosition().x, currentPosition().y)

  import Spawn._

  override def main(): Any = {
    val source = mid % 25 == 0

    def process_logic(procId: Int)(stillSource: Boolean): (Double, Boolean) = {
      val g = classicGradient(mid==procId && stillSource)
      (g, g < 2000 && (mid!=procId || stillSource))
    }

    val procs = spawn[Int,Boolean,Double]((process_logic _), if(source) Set(mid) else Set.empty, source)
      .toList.sortBy(_._2)

    def procName(i: Int) = s"proc${i}"
    val thisNode = alchemistEnvironment.getNodeByID(mid)
    import scala.collection.JavaConverters._
    thisNode.getContents.asScala.foreach( tp => if(tp._1.getName.startsWith("proc")) thisNode.removeConcentration(tp._1) )
    procs.foreach {
      case (pid,value) => node.put(procName(pid), 1)
    }
  }
}