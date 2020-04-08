name := "netty-proxy"

version := "0.1"

scalaVersion := "2.12.11"

// https://mvnrepository.com/artifact/io.netty/netty-all
libraryDependencies += "io.netty" % "netty-all" % "4.1.48.Final"

// https://mvnrepository.com/artifact/com.alibaba/fastjson
libraryDependencies += "com.alibaba" % "fastjson" % "1.2.67"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.30"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-optimize",
  "-release", "14"
)
