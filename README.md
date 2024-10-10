# Weblogic & Kafka Integration

## Weblogic & kafka

### Create a Weblogic Container

https://www.oracle.com/a/ocom/docs/middleware/weblogic-server-on-docker-wp.pdf

docker login container-registry.oracle.com
docker pull  container-registry.oracle.com/java/serverjre:8

download Weblogic Server fila: fmw_12.2.1.4.0_wls_quick_Disk1_1of1.zip
cp fmw_12.2.1.4.0_wls_quick_Disk1_1of1.zip home/nelson/git/docker-images/OracleWebLogic/dockerfiles/12.2.1.4

git clone https://github.com/oracle/docker-images.git

cd home/nelson/git/docker-images/OracleWebLogic/dockerfiles/12.2.1.4

In Dockerfile.developer modify:

	FROM container-registry.oracle.com/java/serverjre:8 as builder

	# Final image stage
	FROM container-registry.oracle.com/java/serverjre:8

cd ..

buildDockerImage.sh -v 12.2.1.4 -d -s


cd /home/nelson/git/docker-images/OracleWebLogic/samples/1213-domain

Modify in Dockerfile file:

	FROM oracle/weblogic:12.2.1.4-developer

	docker build -t oracle/1214-domain --build-arg ADMIN_PASSWORD=weblogic1 .

###  Launch Weblogic & Kafka Containers

docker-compose up -d 

###  Create JMSServer, ConnectionFactory, JMSModule, Queue in weblogic Server

docker-compose exec kafka kafka-topics.sh --create --topic myTopic --partitions 1 --replication-factor 1 --bootstrap-server kafka:9093
docker-compose exec kafka kafka-console-consumer.sh --topic myTopic --from-beginning --bootstrap-server kafka:9093
docker-compose exec kafka kafka-console-producer.sh --topic myTopic  --broker-list kafka:9093

### Launch JMS Sender
/usr/bin/env /usr/local/software/jdk1.8.0_411/bin/java weblogic.jms.weblogic.sender.JMSSender t3://localhost:7001 jms/myConnectionFactory jms/myTestQueue true

