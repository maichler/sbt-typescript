libraryDependencies <+= (sbtVersion) { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
