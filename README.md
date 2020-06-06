# netty-proxy
socks5 代理

## Config
### Server
```json
{
  "listen": 443,
  "key": "123",
  "readTimeout": 300,
  "trafficShaping": {
    "isEnable": true,
    "lowWaterMark": 1048576,
    "highWaterMark": 3145728,
    "delay":300
  }
}
```
- listen: 监听端口
- key: 密钥
- readTimeout: 读取超时连接中断时间(秒)
- trafficShaping: 流控
    - lowWaterMark: 低水位线(字节)
    - highWaterMark: 高水位线(字节)
    - delay: 检测间隔(毫秒)

### Client
```json
{
  "listen": 10080,
  "printHost": true,
  "remote": {
    "US": {
      "connections": 5,
      "host": "domain name",
      "port": 443,
      "key": "123",
      "heartbeatInterval": 30
    },
    "HK": {
      "connections": 5,
      "host": "domain name",
      "port": 443,
      "key": "123",
      "heartbeatInterval": 30
    }
  },
  "trafficShaping": {
    "isEnable": true,
    "lowWaterMark": 5242880,
    "highWaterMark": 20971520,
    "delay": 300
  }
}
```
- listen: 本地socks5协议监听端口
- printHost: 输出代理主机日志
- remote: 代理主机
    - US/HK: 节点名称
    - connections: 连接池容量
    - heartbeatInterval: 心跳包间隔(秒)

## Usages
### Server
```shell script
java -jar proxy.jar server server-config.json
```
### Client
```shell script
java -jar proxy.jar client client-config.json
```
## Compile source code
```shell script
sbt assembly
```

