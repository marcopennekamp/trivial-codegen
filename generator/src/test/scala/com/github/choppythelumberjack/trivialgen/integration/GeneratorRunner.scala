package com.github.choppythelumberjack.trivialgen.integration

import java.nio.file.Paths
import java.time.LocalDateTime

import com.github.choppythelumberjack.trivialgen.generator._
import com.github.choppythelumberjack.trivialgen.{DefaultGenerator, DefaultGeneratorConfiguration, GeneratorConfiguration}

import scala.reflect.runtime.universe.typeOf

/**
  * Generates source files for the integration-tests project.
  */
object GeneratorRunner {

  // TODO: Add multiple trait inheritance test.
  // TODO: Add companion object code generation test.
  // TODO: Add enum types test.
  // TODO: Test "upwards" references in schema model (column -> table and table -> schema).
  // TODO: Test how KeyAsIdColumnPlugin behaves with multi-line primary and foreign keys.

  def main(args: Array[String]): Unit = {
    val path = args(0)
    plumbusAcademy(path)
  }

  /**
    * This configuration tests: Code generation, nullable columns to Options, basic imports, single trait inheritance,
    * KeyAsIdColumnPlugin and the foreign/primary key model.
    */
  def plumbusAcademy(basePath: String): Unit = {
    val config: DefaultGeneratorConfiguration = new DefaultGeneratorConfiguration(
      ConfigLoader.databaseConfiguration("plumbus_academy.sql"),
      sourceSchema = "PUBLIC",
      target = Paths.get(basePath, "plumbus", "academy", "Schema"),
      basePackage = "plumbus.academy",
    ) { configSelf =>
      override val imports: Set[Import] = Set(
        Import.Package("plumbus.academy"), // Testing package imports.
        Import.Entity.fromType(typeOf[LocalDateTime]), // Testing Entity.fromType imports.
        Import.Entity("java.nio.file.Paths"), // Testing fullName "raw" imports.
      )

      override def inheritances: SchemaInheritances = SchemaInheritances(Map(
        "Person" -> TableInheritances(
          // TODO: Test referring to a trait via typeOf.
          fullNames = Seq("plumbus.academy.PersonFunctions"), // Testing simple trait inheritance based on raw names.
        ),
      ))

      override lazy val typeEmitter: TypeEmitter = new ImportSimplifyingTypeEmitter(imports) with KeyAsIdColumnPlugin {
        override protected def config: GeneratorConfiguration = configSelf
      }
    }
    new DefaultGenerator(config).generate()
  }

}
