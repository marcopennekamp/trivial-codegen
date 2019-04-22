package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.schema.Table

/**
  * Handles the transformation of one table to a (case) class.
  */
trait ModelEmitter {
  /**
    * The table to be transformed.
    */
  protected def table: Table

  /**
    * The generated code for the case class. Please ensure that the name is consistent with the naming strategy.
    */
  def code: String = s"case class ${namingStrategy.model(table)}(${properties.mkString(", ")}) $extendsClause"

  /**
    * The emitted extends clause of the case class declaration.
    */
  protected def extendsClause: String = {
    if (supertypes.nonEmpty) {
      (s"extends ${supertypes.head}" :: supertypes.tail.toList).mkString(" with ")
    } else {
      ""
    }
  }

  /**
    * The naming strategy for the model name.
    */
  def namingStrategy: NamingStrategy

  /**
    * The (emitted) properties of the case class.
    */
  protected def properties: Seq[String]

  /**
    * The (emitted) supertypes of the case class.
    */
  protected def supertypes: Seq[String]
}

/**
  * Generates a simple case class based on the configured naming strategy, selected property emitter
  * and inheritance configurations.
  */
class DefaultModelEmitter(
  config: GeneratorConfiguration,
  schemaInheritances: SchemaInheritances,
  override val table: Table
) extends ModelEmitter {
  override def namingStrategy: NamingStrategy = config.namingStrategy
  override def properties: Seq[String] = table.columns.map(c => config.selectPropertyEmitter(c).code)
  override def supertypes: Seq[String] = {
    val inh = schemaInheritances.get(config.namingStrategy.model(table))
    inh.types.map(config.typeEmitter.fromType(_)) ++ inh.fullNames.map(config.typeEmitter.fromName(_))
  }
}