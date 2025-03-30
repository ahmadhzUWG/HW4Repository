@echo off
javac -d . model/*.java
start powershell -Command "java model.Node 1 | Tee-Object -FilePath node1.log"
start powershell -Command "java model.Node 2 | Tee-Object -FilePath node2.log"
start powershell -Command "java model.Node 3 | Tee-Object -FilePath node3.log"
start powershell -Command "java model.Node 4 | Tee-Object -FilePath node4.log"
start powershell -Command "java model.Node 5 | Tee-Object -FilePath node5.log"
