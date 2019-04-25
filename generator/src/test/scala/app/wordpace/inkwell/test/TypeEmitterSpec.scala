package app.wordpace.inkwell.test

import java.time.{LocalDate, LocalDateTime}

import app.wordpace.inkwell.generator.{Import, ImportSimplifyingTypeEmitter, ScalaTypeReference}
import app.wordpace.inkwell.test.TypeEmitterSpec.traits.Trait
import app.wordpace.inkwell.test.TypeEmitterSpec.{CaseClass, ParamAlias, SimpleAlias}
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.runtime.universe.{Type, typeOf}

object TypeEmitterSpec {
  type SimpleAlias = Boolean
  type ParamAlias[A, B] = Map[A, B]
  case class CaseClass()

  object traits {
    trait Trait
  }
}

class TypeEmitterSpec extends FlatSpec with Matchers {

  private def basicEmitter() = new ImportSimplifyingTypeEmitter(Set(
    Import.Package("app.wordpace.inkwell.test.TypeEmitterSpec"),
  ))

  private implicit def toScalaTypeReference(t: Type): ScalaTypeReference = ScalaTypeReference(t)

  "ImportSimplifyingTypeEmitter" should "build primitive types correctly" in {
    val emitter = basicEmitter()
    emitter(typeOf[Boolean]) shouldEqual "Boolean"
    emitter(typeOf[Byte]) shouldEqual "Byte"
    emitter(typeOf[Short]) shouldEqual "Short"
    emitter(typeOf[Int]) shouldEqual "Int"
    emitter(typeOf[Long]) shouldEqual "Long"
    emitter(typeOf[Float]) shouldEqual "Float"
    emitter(typeOf[Double]) shouldEqual "Double"
    emitter(typeOf[Char]) shouldEqual "Char"
  }

  it should "build array types correctly" in {
    val emitter = basicEmitter()
    emitter(typeOf[Array[Boolean]]) shouldEqual "Array[Boolean]"
    emitter(typeOf[Array[Int]]) shouldEqual "Array[Int]"
    emitter(typeOf[Array[String]]) shouldEqual "Array[String]"
    emitter(typeOf[Array[CaseClass]]) shouldEqual "Array[CaseClass]"
  }

  it should "preserve aliases" in {
    val emitter = basicEmitter()
    emitter(typeOf[SimpleAlias]) shouldEqual "SimpleAlias"
    emitter(typeOf[Array[SimpleAlias]]) shouldEqual "Array[SimpleAlias]"
    emitter(typeOf[ParamAlias[String, Seq[String]]]) shouldEqual
     "ParamAlias[String, Seq[String]]"
  }

  it should "simplify imported names correctly" in {
    val emitter = new ImportSimplifyingTypeEmitter(Set(
      Import.Package("app.wordpace.inkwell.test.TypeEmitterSpec"),
      Import.Entity.fromType(typeOf[LocalDateTime]),
    ))

    // CaseClass should be simplified fully while Trait should be simplified up to the traits object.
    emitter(typeOf[CaseClass]) shouldEqual "CaseClass"
    emitter(typeOf[Trait]) shouldEqual "traits.Trait"

    // Only LocalDateTime has been imported as an entity, so LocalDate should need to be fully qualified.
    emitter(typeOf[LocalDateTime]) shouldEqual "LocalDateTime"
    emitter(typeOf[LocalDate]) shouldEqual "java.time.LocalDate"
  }

}
