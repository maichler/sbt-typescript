/*
 * Copyright 2014 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arpnetworking.sbt.typescript

import com.typesafe.sbt.jse.JsEngineImport.JsEngineKeys
import com.typesafe.sbt.web.pipeline.Pipeline
import sbt._
import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.web.SbtWeb
import sbt.Keys._
import spray.json.{JsArray, JsString, JsBoolean, JsObject}

object Import {

  object TypescriptKeys {
    val bundler = TaskKey[Pipeline.Stage]("typescript-bundle", "Bundle typescript output.")
    val bundlePath = SettingKey[String]("typescript-bundle-path", "Relative path to the output bundle file.")
    val bundleBuildDir = SettingKey[File]("typescript-bundle-build-dir", "todo")

    val typescript = TaskKey[Seq[File]]("typescript", "Invoke the typescript compiler.")
    val typescriptGenerateCompiler = TaskKey[File]("generateCompiler", "Generates the typescript compile script.")

    val declaration = SettingKey[Boolean]("typescript-declaration", "Generates corresponding '.d.ts' file.")
    val dependencies = SettingKey[Boolean]("typescript-dependencies", "Outputs a dependency map for typescript files.")
    val sourceMap = SettingKey[Boolean]("typescript-source-map", "Outputs a source map for typescript files.")
    val sourceRoot = SettingKey[String]("typescript-source-root", "Specifies the location where debugger should locate TypeScript files instead of source locations.")
    val mapRoot = SettingKey[String]("typescript-map-root", "Specifies the location where debugger should locate map files instead of generated locations.")
    val target = SettingKey[String]("typescript-target", "ECMAScript target version: 'ES3' (default), or 'ES5'.")
    val noImplicitAny = SettingKey[Boolean]("typescript-no-implicit-any", "Warn on expressions and declarations with an implied 'any' type.")
    val moduleKind = SettingKey[String]("typescript-module", "Specify module code generation: 'commonjs' or 'amd'.")
    val outFile = SettingKey[String]("typescript-output-file", "Concatenate and emit output to a single file.")
    val outDir = SettingKey[String]("typescript-output-directory", "Redirect output structure to the directory.")
    val jsx = SettingKey[String]("typescript-jsx-mode", "Specify JSX mode for .tsx files: 'preserve' (default) or 'react'.")
    val removeComments = SettingKey[Boolean]("typescript-remove-comments", "Remove comments from output.")
  }
}

object SbtTypescript extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport.TypescriptKeys._

  val typescriptUnscopedSettings = Seq(

    includeFilter in typescript := GlobFilter("*.ts") | GlobFilter("*.tsx"),
    excludeFilter in typescript := GlobFilter("*.d.ts"),

    sources in typescript := (sourceDirectories.value ** ((includeFilter in typescript).value -- (excludeFilter in typescript).value)).get,

    jsOptions := JsObject(
      "declaration" -> JsBoolean(declaration.value),
      "dependencies" -> JsBoolean(dependencies.value),
      "sourceMap" -> JsBoolean(sourceMap.value),
      "sourceRoot" -> JsString(sourceRoot.value),
      "mapRoot" -> JsString(mapRoot.value),
      "target" -> JsString(target.value),
      "noImplicitAny" -> JsBoolean(noImplicitAny.value),

      "moduleKind" -> JsString(moduleKind.value),
      "outFile" -> JsString(outFile.value),
      "outDir" -> JsString(outDir.value),
      "removeComments" -> JsBoolean(removeComments.value),
      "jsx" -> JsString(jsx.value),
      "logLevel" -> JsString(logLevel.value.toString)

    ).toString()
  )

  override def projectSettings = Seq(

    declaration := false,
    dependencies := false,
    sourceMap := false,
    sourceRoot := "",
    mapRoot := "",
    target := "ES5",
    noImplicitAny := false,
    moduleKind := "",
    outFile := "",
    outDir := ((webTarget in Assets).value / "typescript").absolutePath,
    removeComments := false,
    jsx := "preserve",
    JsEngineKeys.parallelism := 1,
    logLevel := Level.Info

  ) ++ inTask(typescript)(
    SbtJsTask.jsTaskSpecificUnscopedSettings ++
      inConfig(Assets)(typescriptUnscopedSettings) ++
      inConfig(TestAssets)(typescriptUnscopedSettings) ++
      Seq(
        moduleName := "typescript",
        shellFile := getClass.getClassLoader.getResource("typescriptc.js"),

        taskMessage in Assets := "TypeScript compiling",
        taskMessage in TestAssets := "TypeScript test compiling"
      )
  ) ++ SbtJsTask.addJsSourceFileTasks(typescript) ++ Seq(
    typescript in Assets := (typescript in Assets).dependsOn(webModules in Assets).value,
    typescript in TestAssets := (typescript in TestAssets).dependsOn(webModules in TestAssets).value
  )

}

object SbtBundle extends AutoPlugin {

  override def requires = SbtTypescript

  override def trigger = AllRequirements

  import com.typesafe.sbt.jse.SbtJsEngine.autoImport.JsEngineKeys._
  import com.typesafe.sbt.jse.SbtJsTask.JsTaskProtocol._
  import com.typesafe.sbt.jse.SbtJsTask.autoImport.JsTaskKeys._
  import com.typesafe.sbt.web.Import.WebKeys._
  import com.typesafe.sbt.web.SbtWeb.autoImport._
  import com.arpnetworking.sbt.typescript.SbtTypescript.autoImport.TypescriptKeys._

  override def projectSettings = Seq(

    bundlePath := "javascripts/bundle.js",
    bundleBuildDir := (resourceManaged in bundler).value / "build",
    excludeFilter in bundler := HiddenFileFilter || "*.d.ts",
    includeFilter in bundler := "*.deps" || "*.js" || "*.js.map" || "*.ts",
    resourceManaged in bundler := webTarget.value / bundler.key.label,
    bundler := runBundler.dependsOn(nodeModules in Assets).value,
    jsOptions := JsObject(
      "bundlePath" -> JsString(bundlePath.value),
      "declaration" -> JsBoolean(declaration.value),
      "dependencies" -> JsBoolean(dependencies.value),
      "environment" -> JsString(if(appConfiguration.value.arguments().contains("dist")) "prod" else "dev"),
      "logLevel" -> JsString(logLevel.value.toString),
      "moduleKind" -> JsString(moduleKind.value),
      "sourceMap" -> JsBoolean(sourceMap.value)
    ).toString(),
    shellSource := {
      SbtWeb.copyResourceTo(
        (resourceManaged in bundler).value,
        getClass.getClassLoader.getResource("typescript-bundler.js"),
        streams.value.cacheDirectory / "copy-resource"
      )
    }
  )

  private def buildSourceTargetArg(srcTarget: Seq[(File,String)]): String = {
    JsArray(srcTarget.map( c => {
      JsObject(
        "source" -> JsString(c._1.getPath),
        "target" -> JsString(c._2))
    }).toList).toString()
  }

  private def runBundler: Def.Initialize[Task[Pipeline.Stage]] = Def.task {

    mappings =>

      val include = (includeFilter in bundler).value
      val exclude = (excludeFilter in bundler).value

      val preMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))

      val cacheDirectory = streams.value.cacheDirectory / bundler.key.label
      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
        inputFiles =>

          val targetPath = bundleBuildDir.value.getPath
          val nodeModules = (nodeModuleDirectories in Plugin).value.map(_.getPath)
          val args = Seq(buildSourceTargetArg(preMappings), targetPath, jsOptions.value)
          val timeout = (timeoutPerSource in bundler).value * (if (preMappings.size < 1) 1 else preMappings.size)

          if (preMappings.nonEmpty) {
            val size = preMappings.count(p => p._2.endsWith(".js") || p._2.endsWith(".jsx"))
            streams.value.log.info("TypeScript bundling on " + size + " source(s)")
          }

          SbtJsTask.executeJs(
            state.value,
            (engineType in bundler).value,
            (command in bundler).value,
            nodeModules,
            shellSource.value,
            args,
            timeout
          ).map {
            result => result.convertTo[List[File]]
          }.head.toSet
      }

      val postMappings = runUpdate(preMappings.map(c => c._1).toSet).pair(relativeTo(bundleBuildDir.value))
      (mappings.toSet -- preMappings.toSet ++ postMappings.toSet).toSeq
  }
}