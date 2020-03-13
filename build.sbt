name := "netty-proxy"

version := "0.1"

scalaVersion := "2.12.10"

// https://mvnrepository.com/artifact/io.netty/netty-all
libraryDependencies += "io.netty" % "netty-all" % "4.1.47.Final"

// https://mvnrepository.com/artifact/com.alibaba/fastjson
libraryDependencies += "com.alibaba" % "fastjson" % "1.2.66"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-optimize",
  "-release", "11"
)
