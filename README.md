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

## Решение

