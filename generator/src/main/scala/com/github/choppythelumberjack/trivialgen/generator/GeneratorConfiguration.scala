package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.schema.{Column, DefaultSchemaReader, DefaultTypeResolver, SchemaReader, Table, TypeResolver}
import com.github.choppythelumberjack.trivialgen._
import com.github.choppythelumberjack.trivialgen.generator.DefaultGeneratorConfiguration.Import
import com.github.choppythelumberjack.trivialgen.generator.DefaultModelEmitter.InheritanceMap

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
    * The target folder to which Scala files are generated.
    */
  def targetFolder: String

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
    * The raw type builder can be overridden to change how type names are turned to strings globally, instead
    * of overriding a specific emitter's rawType definition.
    */
  def rawTypeBuilder: RawTypeBuilder

  /**
    * The generator's schema emitter, which is used to
    */
  def schemaEmitter: SchemaEmitter

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

  def querySchemaImports: String = ""

  def packagingStrategy: PackagingStrategy
  def memberNamer: MemberNamer
}

case class DefaultGeneratorConfiguration(
  override val db: DatabaseConfiguration,
  override val sourceSchema: String,
  override val targetFolder: String,
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

  /**
    * A set of imported classes which will be accessible (i.e. types built by the raw type builder) and
    * generally available (by "plain text" generated code, such as companion objects' innerCode) by simple
    * name in the generated code.
    */
  def imports: Set[Import] = Set.empty

  override val ignoredTables: Set[String] = Set.empty
  override val schemaReader: SchemaReader = new DefaultSchemaReader(this)
  override val typeResolver: TypeResolver = new DefaultTypeResolver(customTypes)
  override val namingStrategy: NamingStrategy = CamelCase
  override val rawTypeBuilder: RawTypeBuilder = new ImportSimplifyingRawTypeBuilder(imports)

  override def selectModelEmitter(table: Table): ModelEmitter = new DefaultModelEmitter(this, inheritanceMap, table)
  override def selectCompanionEmitter(table: Table): CompanionEmitter = new DefaultCompanionEmitter(this, table)
  override def selectPropertyEmitter(column: Column): PropertyEmitter = new DefaultPropertyEmitter(this, column)

  def packagePrefix: String

  def packagingStrategy: PackagingStrategy = PackagingStrategy.ByPackageHeader.TablePerFile(packagePrefix)

  /**
    * When defining your query schema object, this will name the method which produces the query schema.
    * It will be named <code>query</code> by default so if you are doing Table Stereotyping, be sure
    * it's something reasonable like <code>(ts) => ts.tableName.snakeToLowerCamel</code>
    *
    * <pre>{@code
    * case class Person(firstName:String, lastName:String, age:Int)
  *
  * object Person {
  *   // The method will be 'query' by default which is good if you are not stereotyping.
  *   def query = querySchema[Person](...)
  * }
    * }</pre>
    *
    * Now let's take an example where you have a database that has two schemas <code>ALPHA</code> and <code>BRAVO</code>,
    * each with a table called Person and you want to stereotype the two schemas into one table case class.
    * In this case you have to be sure that memberNamer is something like <code>(ts) => ts.tableName.snakeToLowerCamel</code>
    * so you'll get a different method for every querySchema.
    *
    * <pre>{@code
    * case class Person(firstName:String, lastName:String, age:Int)
  *
  * object Person {
  *   // Taking ts.tableName.snakeToLowerCamel will ensure each one has a different name. Otherise
  *   // all of them will be 'query' which will result in a compile error.
  *   def alphaPerson = querySchema[Person]("ALPHA.PERSON", ...)
  *   def bravoPerson = querySchema[Person]("BRAVO.PERSON", ...)
  * }
    * }</pre>
    */
  def memberNamer: MemberNamer = (ts) => "query" //ts.tableName.snakeToLowerCamel
}
