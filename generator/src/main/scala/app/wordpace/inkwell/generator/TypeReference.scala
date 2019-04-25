package app.wordpace.inkwell.generator

import app.wordpace.inkwell.generator.TypeUtil.TypeExtensions

import scala.reflect.runtime.universe.{Type, typeOf}

/**
  * An abstract type reference used by [[TypeEmitter]] to resolve a type.
  */
trait TypeReference {
  def fullName: String
  def typeArguments: Seq[TypeReference]
}

/**
  * A type reference pointing to a Scala [[Type]], which is the most natural and safe representation possible.
  * However, a [[Type]] is not always available, in which case you need to choose a different representation.
  *
  * @example Use this class as follows:
  *          ```
  *          import scala.reflect.runtime.universe.typeOf
  *          ScalaTypeReference(typeOf[MyType])
  *          ```
  */
case class ScalaTypeReference(t: Type) extends TypeReference {
  override def fullName: String = t.symbolPreserveAliases.fullName
  override def typeArguments: Seq[TypeReference] = t.typeArgs.map(ScalaTypeReference)
}

/**
  * A type reference pointing to a type which can not yet be referenced via [[typeOf]], which is the case when
  * you need to reference types that are either generated by inkwell or depend on types generated by inkwell.
  *
  * @example Let's say the application (using inkwell) defines an `Id[A]` type, which represents IDs for any
  *          type `A`. Say you have a table `person` from which a case class `Person` is generated. You can
  *          resolve the JDBC type of its ID column to `Id`, but you can not provide the type argument
  *          `A = Person` since `Person` does not exist until code generation is finished. In such a case,
  *          this class can be used to represent the right type for the property.
  */
case class NamedTypeReference(
  override val fullName: String,
  override val typeArguments: Seq[TypeReference] = Seq.empty
) extends TypeReference
