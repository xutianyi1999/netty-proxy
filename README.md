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
    "delay": 100
  }
}
```
- listen: 监听端口
- key: 密钥
- readTimeout: 读取超时连接中断时间(秒)
- trafficShaping: 流控
- delay: 检测间隔(毫秒)

### Client
```json
{
  "listen": 10080,
  "remote": {
    "US": {
      "connections": 5,
      "host": "domain name",
      "port": 443,
      "key": "123"
    },
    "HK": {
      "connections": 5,
      "host": "domain name",
      "port": 443,
      "key": "123"
    }
  },
  "trafficShaping": {
    "isEnable": true,
    "delay": 100
  }
}
```
- listen: 本地socks5协议监听端口
- US/HK: 节点名称
- connections: 连接池大小

## Usages
### Server
```shell script
java -jar proxy.jar client client-config.json
```
### Client
```shell script
java -jar proxy.jar server server-config.json
```
## Compile source code
```shell script
sbt assembly
```
