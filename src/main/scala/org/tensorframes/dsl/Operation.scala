package org.tensorframes.dsl

import org.apache.spark.sql.types.NumericType
import org.tensorflow.framework.{NodeDef, AttrValue}
import org.tensorframes.{dsl => tf, ShapeDescription, Shape}
import scala.collection.JavaConverters._

/**
 * A node in the TensorFlow graph.
 */
trait Operation {
  /**
   * The path of the operation. It follows TensorFlow's path conventions.
 *
   * @return
   */
  def name: String

  /**
   * The name of the operation.
 *
   * @return
   */
  def opName: String

  /**
   * The dimensions of the default tensor output of this operation.
   *
   * @return
   */
  def dims: Seq[Int]

  def named(newName: String): Operation

  def +(other: Operation): Operation = tf.add(this, other)
}

/**
 * Implementation of an operation.
 */
private[tensorframes] case class Node(
    name: String,
    opName: String,
    scalarType: NumericType,
    shape: Shape,
    parents: Seq[Node],
    isOp: Boolean,
    extraAttr: Map[String, AttrValue]) extends Operation {

  import ProtoConversions._
  import DslImpl._

  override def dims: Seq[Int] = shape.dims.map(_.toInt)

  def node: NodeDef = {
    val b = NodeDef.newBuilder()
    b.setName(name)
    b.setOp(opName)
    parents.foreach(p => b.addInput(p.name))
    if (isOp) {
      b.putAllAttr(Map("T" -> getDType(scalarType).toAttr).asJava)
    } else {
      b.putAllAttr(Map(
        "dtype" -> getDType(scalarType).toAttr).asJava)
    }
    b.putAllAttr(extraAttr.asJava)
    b.build()
  }

  override def named(newName: String): Node = copy(name = newName)

}

private[dsl] object Node {
  def hints(ns: Seq[Node]): ShapeDescription = {
    val m = ns.map { n =>
      n.name -> n.shape
    } .toMap
    val f = ns.map(_.name)
    ShapeDescription(m, f)
  }
}
