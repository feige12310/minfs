#!/bin/bash
#一键启动脚本
#启动metaserver 日志输出到log 添加zookeeper -Dzookeeper.addr=10.243.137.17
nohup java -Dzookeeper.addr=127.0.0.1:2181 -jar ../metaServer/metaServer-1.0.jar --server.port=8001 > "../metaServer/meta1.log" 2>&1 &
#启动从metaserver指定8001端口--server.port=8001  -Dzookeeper.addr=10.243.137.17
nohup java -Dzookeeper.addr=127.0.0.1:2181 -jar ../metaServer/metaServer-1.0.jar --server.port=8002 > "../metaServer/meta2.log" 2>&1 &
#启动dataserver 9000 -9003
nohup java -Dzookeeper.addr=127.0.0.1:2181 -jar ../dataServer/dataServer-1.0.jar --server.port=9001 --az.rack=rack1 --az.zone=zone1 > "../dataServer/data1.log" 2>&1 &
nohup java -Dzookeeper.addr=127.0.0.1:2181 -jar ../dataServer/dataServer-1.0.jar --server.port=9002 --az.rack=rack1 --az.zone=zone2 > "../dataServer/data2.log" 2>&1 &
nohup java -Dzookeeper.addr=127.0.0.1:2181 -jar ../dataServer/dataServer-1.0.jar --server.port=9003 --az.rack=rock2 --az.zone=zone1 > "../dataServer/data3.log" 2>&1 &
nohup java -Dzookeeper.addr=127.0.0.1:2181 -jar ../dataServer/dataServer-1.0.jar --server.port=9004 --az.rack=rock2 --az.zone=zone2 > "../dataServer/data4.log" 2>&1 &
