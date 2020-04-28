name := "netty-proxy"

version := "0.1"

scalaVersion := "2.12.11"

val nettyVersion = "4.1.49.Final"

// https://mvnrepository.com/artifact/io.netty/netty-handler
libraryDependencies += "io.netty" % "netty-handler" % nettyVersion

// https://mvnrepository.com/artifact/io.netty/netty-codec-socks
libraryDependencies += "io.netty" % "netty-codec-socks" % nettyVersion

// https://mvnrepository.com/artifact/io.netty/netty-transport-native-epoll
libraryDependencies += "io.netty" % "netty-transport-native-epoll" % nettyVersion classifier "linux-x86_64"

// https://mvnrepository.com/artifact/com.alibaba/fastjson
libraryDependencies += "com.alibaba" % "fastjson" % "1.2.68"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.30"

assemblyMergeStrategy in assembly := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-optimize",
  "-release", "11"
)
