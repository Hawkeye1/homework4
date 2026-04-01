# Практическая работа модуля 4: Интеграция с внешними системами на примере Kafka Connect

**Цель практической работы** - закрепить знания о работе с коннекторами, а также научиться собирать и анализировать метрики.

## Задание 1. Настройка Debezium Connector для PostgreSQL
1. Создайте файл docker-compose.yaml. В нём должны присутствовать следующие компоненты:
  * Apache Kafka
  * Kafka Connect
  * PostgreSQL
2. Создайте базу данных PostgreSQL и таблицы, которые будут использоваться для работы — **users** и **orders** со следующими структурами:
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id),
    product_name VARCHAR(100),
    quantity INT,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
); 
```
3.  Настройте Debezium Connector для отслеживания изменений ТОЛЬКО в таблицах **users** и **orders**.
4.  Проверьте статус коннектора и убедитесь, что он работает корректно.
5.  Получите данные из топиков Apache Kafka, используя код на выбранном вами языке программирования, и выведите их в терминал.

## Задание 2. Мониторинг и анализ метрик
1. Настройте Prometheus для сбора метрик из Kafka Connect.
2. Создайте графики в Grafana для визуализации метрик передачи данных и мониторинга работоспособности Kafka Connector.

## Описание компонентов системы

**docker-compose.yaml** содержит следующие компоненты системы:
* Три брокера (`kafka-0`, `kafka-1` и `kafka-2`)
* UI интерфейс подключенный к брокерам для удобного просмотра данных в топиках
* Schema Registry подключенная к брокерам, что обеспечивает согласованность данных
* База данных postgres как источник данных
* Kafka Connect для пропагирования данных из базы данных postgres в Kafka
* Prometheus для сбора метрик Kafka Connect и Kafka
* Grafana для визуализации метрик

## Результат эксперемента

| Эксперимент | batch.size | linger.ms | compression.type | buffer.memory | Source Record Write Rate (кops/sec) |
|:-----------:|:----------:|:---------:|:----------------:|:-------------:|:-----------------------------------:|
|1            |500         |1000       |none              |33554432       |85.4                                 |
|2            |6930        |1000       |none              |33554432       |159                                  |
|3            |63000       |100        |none              |33554432       |159                                  |
|4            |63000       |100        |none              |67108864       |159                                  |
|5            |63000       |100        |lz4               |67108864       |159                                  |

Вывод: Увеличение batch.size позволило улучшить производительность системы.
Но изменение linger.ms, compression.type и buffer.memory не привели к изменениям производительности. Есть подозрения что коннектр настроен некорректно.
Пример:
    
```bash
    curl -X PUT \
    -H "Content-Type: application/json" \
    --data '{
    "connector.class":"io.confluent.connect.jdbc.JdbcSourceConnector",
    "tasks.max":"1",
    "connection.url":"jdbc:postgresql://postgres:5432/customers?user=postgres-user&password=postgres-pw&useSSL=false",
    "connection.attempts":"5",
    "connection.backoff.ms":"50000",
    "mode":"timestamp",
    "timestamp.column.name":"updated_at",
    "topic.prefix":"postgresql-jdbc-bulk-",
    "table.whitelist": "users",
    "poll.interval.ms": "200",
    "batch.max.rows": 100,
    "producer.override.linger.ms": 100,
    "producer.override.batch.size": 63000,
    "producer.override.buffer.memory": 67108864,
    "producer.override.compression.type": "lz4",
    "transforms":"MaskField",
    "transforms.MaskField.type":"org.apache.kafka.connect.transforms.MaskField$Value",
    "transforms.MaskField.fields":"private_info",
    "transforms.MaskField.replacement":"CENSORED"
    }' \
    http://localhost:8083/connectors/postgres-source/config | jq
```

## Настройки Debezium Connector

```json
    {
      "name": "pg-connector",
      "config": {
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "database.hostname": "postgres",
        "database.port": "5432",
        "database.user": "postgres-user",
        "database.password": "postgres-pw",
        "database.dbname": "customers",
        "database.server.name": "customers",
        "table.whitelist": "users,orders",
        "transforms": "unwrap",
        "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
        "transforms.unwrap.drop.tombstones": "false",
        "transforms.unwrap.delete.handling.mode": "rewrite",
        "topic.prefix": "customers",
        "topic.creation.enable": "true",
        "topic.creation.default.replication.factor": "-1",
        "topic.creation.default.partitions": "-1",
        "skipped.operations": "none"
      }
    }
```


## Решение
1. **Настройка Debezium Connector для PostgreSQL**
    
    Запускаем Docker compose (локальный терминал):
    ```bash
    docker compose up -d
    ```
    
    Проверяем что контейнеры запущены (локальный терминал):
    ```bash
    docker ps
    ```
    
    Ожидаемый вывод:
    ```
    CONTAINER ID   IMAGE                               COMMAND                  CREATED          STATUS                    PORTS                                                                                                          NAMES
    c6bf78ae9a0b   prom/prometheus:v2.30.3             "/bin/prometheus --w…"   11 minutes ago   Up 11 minutes             0.0.0.0:9090->9090/tcp, [::]:9090->9090/tcp                                                                    homework4_prometheus_1
    95bba13a4baa   homework4_kafka-connect             "/etc/confluent/dock…"   11 minutes ago   Up 11 minutes (healthy)   0.0.0.0:8083->8083/tcp, [::]:8083->8083/tcp, 0.0.0.0:9875-9876->9875-9876/tcp, [::]:9875-9876->9875-9876/tcp   kafka-connect
    267a5d520685   bitnamilegacy/schema-registry:7.6   "/opt/bitnami/script…"   11 minutes ago   Up 11 minutes             127.0.0.1:8081->8081/tcp                                                                                       homework4_schema-registry_1
    067e78c7946a   debezium/postgres:16                "docker-entrypoint.s…"   11 minutes ago   Up 11 minutes             0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp                                                                    postgres
    e837fe712341   homework4_grafana                   "/run.sh"                11 minutes ago   Up 11 minutes             0.0.0.0:3000->3000/tcp, [::]:3000->3000/tcp                                                                    homework4_grafana_1
    a5ccacb418a5   provectuslabs/kafka-ui:v0.7.0       "/bin/sh -c 'java --…"   11 minutes ago   Up 11 minutes             127.0.0.1:8080->8080/tcp                                                                                       homework4_ui_1
    735b0dd896b6   bitnamilegacy/kafka:3.7             "/opt/bitnami/script…"   11 minutes ago   Up 11 minutes             127.0.0.1:9094->9094/tcp                                                                                       kafka-0
    f57df6f078ff   bitnamilegacy/kafka:3.7             "/opt/bitnami/script…"   11 minutes ago   Up 11 minutes             0.0.0.0:9095->9095/tcp, [::]:9095->9095/tcp                                                                    kafka-1
    557159fa251e   bitnamilegacy/kafka:3.7             "/opt/bitnami/script…"   11 minutes ago   Up 11 minutes             0.0.0.0:9096->9096/tcp, [::]:9096->9096/tcp                                                                    kafka-2
    ```
    
    Вызываем psql внутри запущенного postgres контейнера:
    ```bash
    docker exec -it postgres psql -h 127.0.0.1 -U postgres-user -d customers 
    ```
    
    Создаём таблицы `users` и `orders` (psql):
    ```sql
    CREATE TABLE users (
        id SERIAL PRIMARY KEY,
        name VARCHAR(100),
        email VARCHAR(100),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    
    
    CREATE TABLE orders (
        id SERIAL PRIMARY KEY,
        user_id INT REFERENCES users(id),
        product_name VARCHAR(100),
        quantity INT,
        order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ); 
    ``` 

    Ожидаемый вывод:
    ```
    CREATE TABLE
    CREATE TABLE
    ```
   
    Проверяем что нам доступен debezium плагин для Kafka Connect (локальный терминал):
    ```bash
    curl localhost:8083/connector-plugins | jq
    ```
    
    Ожидаемый результат:
    ```json
    [
      {
        "class": "io.debezium.connector.postgresql.PostgresConnector",
        "type": "source",
        "version": "3.4.2.Final"
      },
      {
        "class": "org.apache.kafka.connect.mirror.MirrorCheckpointConnector",
        "type": "source",
        "version": "7.7.1-ccs"
      },
      {
        "class": "org.apache.kafka.connect.mirror.MirrorHeartbeatConnector",
        "type": "source",
        "version": "7.7.1-ccs"
      },
      {
        "class": "org.apache.kafka.connect.mirror.MirrorSourceConnector",
        "type": "source",
        "version": "7.7.1-ccs"
      }
    ]
    ```

    Создаём Kafka Connect (локальный терминал):
    ```bash
    curl -X POST -H 'Content-Type: application/json' --data @connector.json http://localhost:8083/connectors | jq
    ```
    
    Проверяем статус (локальный терминал):
    ```bash
    curl http://localhost:8083/connectors/pg-connector/status | jq
    ```
    
    Ожидаемый результат:
    ```json
    {
      "name": "pg-connector",
      "connector": {
        "state": "RUNNING",
        "worker_id": "localhost:8083"
      },
      "tasks": [
        {
          "id": 0,
          "state": "RUNNING",
          "worker_id": "localhost:8083"
        }
      ],
      "type": "source"
    }
    ```
    
    Запускаем терминал в папке /SimpleConsumer и собираем проект (локальный терминал):
    ```bash
    sudo ./mvnw clean package
    ```
    
    Ожидаемый вывод:
    ```
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time:  04:01 min
    [INFO] Finished at: 2026-04-01T23:29:23+03:00
    [INFO] ------------------------------------------------------------------------
    ```
    
    Запускаем наше приложение (локальный терминал):
    ```bash
    java -jar target/hw4-0.0.1-SNAPSHOT.jar
    ```
    
    Вызываем psql внутри запущенного postgres контейнера:
    ```bash
    docker exec -it postgres psql -h 127.0.0.1 -U postgres-user -d customers 
    ```
    
    Добавляем тестовые данные в таблицы **users** и **orders** (psql):
    ```sql
    -- Добавление пользователей
    INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com');
    INSERT INTO users (name, email) VALUES ('Jane Smith', 'jane@example.com');
    INSERT INTO users (name, email) VALUES ('Alice Johnson', 'alice@example.com');
    INSERT INTO users (name, email) VALUES ('Bob Brown', 'bob@example.com');
    -- Добавление заказов
    INSERT INTO orders (user_id, product_name, quantity) VALUES (1, 'Product A', 2);
    INSERT INTO orders (user_id, product_name, quantity) VALUES (1, 'Product B', 1);
    INSERT INTO orders (user_id, product_name, quantity) VALUES (2, 'Product C', 5);
    INSERT INTO orders (user_id, product_name, quantity) VALUES (3, 'Product D', 3);
    INSERT INTO orders (user_id, product_name, quantity) VALUES (4, 'Product E', 4);
    ```
    
    Ожидаемый результат:
    ```
    INSERT 0 1
    INSERT 0 1
    INSERT 0 1
    INSERT 0 1
    INSERT 0 1
    INSERT 0 1
    INSERT 0 1
    INSERT 0 1
    INSERT 0 1
    ```
    
    Ожидаемый результат в терминале с запущенным приложением: 
    ```
    Received message: {"id": 1, "name": "John Doe", "email": "john@example.com", "created_at": 1775075948478460}
    Received message: {"id": 2, "name": "Jane Smith", "email": "jane@example.com", "created_at": 1775075948480770}
    Received message: {"id": 3, "name": "Alice Johnson", "email": "alice@example.com", "created_at": 1775075948482603}
    Received message: {"id": 4, "name": "Bob Brown", "email": "bob@example.com", "created_at": 1775075948484449}
    Received message: {"id": 1, "user_id": 1, "product_name": "Product A", "quantity": 2, "order_date": 1775075948486150}
    Received message: {"id": 2, "user_id": 1, "product_name": "Product B", "quantity": 1, "order_date": 1775075948488343}
    Received message: {"id": 3, "user_id": 2, "product_name": "Product C", "quantity": 5, "order_date": 1775075948489837}
    Received message: {"id": 4, "user_id": 3, "product_name": "Product D", "quantity": 3, "order_date": 1775075948491586}
    Received message: {"id": 5, "user_id": 4, "product_name": "Product E", "quantity": 4, "order_date": 1775075948493280}
    ```
    
2. **Мониторинг и анализ метрик**

    Откроем grafana в браузере: http://localhost:3000/
    
    login/pass: **admin/admin**
    
    Далее импортируем наш dashboard:
    * В подменю Create (занчок +) выберем пункт Import
    * В загрузившейся вкладке нажимаем на кнопку Upload JSON file и выбираем файл /homework4/grafana/dashboards/Kafka Connect Overview-HW4.json
    
