package com.github.choppythelumberjack.trivialgen

import java.nio.file.Path

import com.github.choppythelumberjack.trivialgen.generator.DefaultModelEmitter.InheritanceMap
import com.github.choppythelumberjack.trivialgen.generator._
import com.github.choppythelumberjack.trivialgen.schema._

import scala.reflect.ClassTag

case class DatabaseConfiguration(
  url: String,
  username: String,
  password: String,
)

/**
  * Basic and advanced configuration options that can be modified without extending the generator class.
  *
  * In most cases, extend [[DefaultGeneratorConfiguration]] instead of this trait directly.
  */
trait GeneratorConfiguration {
  /**
    * The [[DatabaseConfiguration]] used to access your local database.
    */
  def db: DatabaseConfiguration

  /**
    * The name of the schema used as the basis for code generation.
    */
  def sourceSchema: String

  /**
    * The target folder or <b>extensionless</b> target file (depending on your [[SchemaEmitter]]).
    *
    * For example, [[SingleFileSchemaEmitter]] assumes the target to be a file.
    */
  def target: Path

  /**
    * The base package of the target.
    */
  def basePackage: String

  /**
    * Names of all tables that should be ignored during code generation.
    */
  def ignoredTables: Set[String]

  /**
    * The schema reader fetches the schema from the database and transforms it into a schema model. You generally don't
    * need to override this, but the option is there just in case.
    */
  def schemaReader: SchemaReader

  /**
    * The type resolver translates JDBC types to Scala types.
    */
  def typeResolver: TypeResolver

  /**
    * The naming strategy turns SQL names into Scala names for tables and columns (classes and attributes).
    * You can use one of the provided naming strategies or implement your own.
    */
  def namingStrategy: NamingStrategy

  /**
    * A set of imported classes and packages which will be accessible by simple name in the generated code.
    */
  def imports: Set[Import]

  /**
    * The raw type builder can be overridden to change how type names are turned to strings globally, instead
    * of overriding a specific emitter's rawType definition.
    */
  def rawTypeBuilder: RawTypeBuilder

  /**
    * Selects the schema emitter based on the given schema.
    */
  def selectSchemaEmitter(schema: Schema): SchemaEmitter

  /**
    * Selects the model emitter based on the given table (and possibly the schema).
    */
  def selectModelEmitter(table: Table): ModelEmitter

  /**
    * Selects the companion emitter based on the given table (and possibly the schema).
    */
  def selectCompanionEmitter(table: Table): CompanionEmitter

  /**
    * Selects the property emitter based on the given column (and possibly table and even schema).
    */
  def selectPropertyEmitter(column: Column): PropertyEmitter
}

case class DefaultGeneratorConfiguration(
  override val db: DatabaseConfiguration,
  override val sourceSchema: String,
  override val target: Path,
  override val basePackage: String,
) extends GeneratorConfiguration {

  /**
    * A map of custom JDBC to Scala type mappings, used by the [[DefaultTypeResolver]]. Note that this
    * map is not used if you override [[typeResolver]].
    */
  def customTypes: Map[String, ClassTag[_]] = Map.empty

  /**
    * The inheritance map is used by the [[DefaultModelEmitter]] to provide support for model supertypes.
    * You can override this value to provide your own inheritance rules. Please note that the left-hand
    * values of the map are <b>SQL names</b> not Scala names (since the latter is not stable due to a
    * possible variance in naming strategies).
    */
  def inheritanceMap: InheritanceMap = Map.empty

  override val ignoredTables: Set[String] = Set.empty
  override val schemaReader: SchemaReader = new DefaultSchemaReader(this)
  override val typeResolver: TypeResolver = new DefaultTypeResolver(customTypes)
  override val namingStrategy: NamingStrategy = CamelCase
  override val imports: Set[Import] = Set.empty
  override val rawTypeBuilder: RawTypeBuilder = new ImportSimplifyingRawTypeBuilder(imports)

  override def selectSchemaEmitter(schema: Schema): SchemaEmitter = new SingleFileSchemaEmitter(this, schema)
  override def selectModelEmitter(table: Table): ModelEmitter = new DefaultModelEmitter(this, inheritanceMap, table)
  override def selectCompanionEmitter(table: Table): CompanionEmitter = new DefaultCompanionEmitter(this, table)
  override def selectPropertyEmitter(column: Column): PropertyEmitter = new DefaultPropertyEmitter(this, column)
}