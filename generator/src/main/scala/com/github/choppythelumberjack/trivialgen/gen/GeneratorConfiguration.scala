package com.github.choppythelumberjack.trivialgen.gen

import com.github.choppythelumberjack.trivialgen.schema.{DefaultJdbcTypeResolver, DefaultSchemaReader, JdbcTypeResolver, SchemaReader}
import com.github.choppythelumberjack.trivialgen._

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
    * The type resolver translates JDBC types to Scala types. Define your own to support custom types.
    */
  def typeResolver: JdbcTypeResolver

  /**
    * The schema reader fetches the schema from the database and transforms it into a schema model. You generally don't
    * need to override this, but the option is there just in case.
    */
  def schemaReader: SchemaReader

  def querySchemaImports: String = ""
  def nameParser: NameParser
  def unrecognizedTypeStrategy: UnrecognizedTypeStrategy


  def packagingStrategy: PackagingStrategy
  def memberNamer: MemberNamer
}

case class DefaultGeneratorConfiguration(
  override val db: DatabaseConfiguration,
  override val sourceSchema: String,
  override val targetFolder: String,
) extends GeneratorConfiguration {

  override val ignoredTables = Set.empty
  override val typeResolver = new DefaultJdbcTypeResolver()
  override val schemaReader = new DefaultSchemaReader(this)

  def packagePrefix:String

  def nameParser: NameParser = LiteralNames

  /**
    * When the code generator uses the Jdbc Typer to figure out which Scala/Java objects to use for
    * which JDBC type (e.g. use String for Varchar(...), Long for bigint etc...),
    * what do we do when we discover a JDBC type which we cannot translate (e.g. blob which is
    * currently not supported by quill). The simplest thing to do is to skip the column.
    */
  def unrecognizedTypeStrategy:UnrecognizedTypeStrategy = SkipColumn

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