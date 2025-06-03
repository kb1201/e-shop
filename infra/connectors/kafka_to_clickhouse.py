import json
import logging
from datetime import datetime
from typing import Dict, Any, Optional
import sys
import signal
import time
import threading
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal

from kafka import KafkaConsumer
from clickhouse_driver import Client
from clickhouse_driver.errors import Error as ClickHouseError


class TopicProcessor:
    def __init__(self, topic: str, kafka_config: Dict[str, Any], clickhouse_config: Dict[str, Any]):
        self.topic = topic
        self.kafka_config = kafka_config
        self.clickhouse_config = clickhouse_config
        self.running = True
        self.logger = logging.getLogger(f"{__name__}.{topic}")

    def determine_table_name(self):
        if 'inventory' in self.topic:
            return "analytics.inventory_fact"
        elif 'shipment' in self.topic:
            return "analytics.shipment_fact"
        elif 'cart_items' in self.topic:
            return "analytics.cart_items_fact"
        elif 'order_items' in self.topic:
            return "analytics.order_items_fact"
        elif 'orders' in self.topic and 'cart_items' not in self.topic and 'order_items' not in self.topic:
            return "analytics.orders_fact"
        else:
            raise ValueError(f"Unsupported topic: {self.topic}")

    def transform_message(self, message: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Transform message based on topic"""
        if 'inventory' in self.topic:
            return self._transform_inventory_message(message)
        elif 'shipment' in self.topic:
            return self._transform_shipment_message(message)
        elif 'cart_items' in self.topic:
            return self._transform_cart_items_message(message)
        elif 'order_items' in self.topic:
            return self._transform_order_items_message(message)
        elif 'orders' in self.topic and 'cart_items' not in self.topic and 'order_items' not in self.topic:
            return self._transform_orders_message(message)
        return None

    def _transform_inventory_message(self, message: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Inventory message transformation"""
        try:
            if message.get('__deleted', 'false').lower() == 'true':
                self.logger.info(f"Skipping deleted inventory record: {message.get('id')}")
                return None

            last_updated_micros = message.get('last_updated', 0)
            last_updated = datetime.fromtimestamp(
                last_updated_micros / 1_000_000) if last_updated_micros else datetime.now()

            return {
                'id': int(message.get('id', 0)),
                'product_id': int(message.get('product_id', 0)),
                'sku': str(message.get('sku', '')),
                'quantity_available': int(message.get('quantity_available', 0)),
                'reserved_quantity': int(message.get('reserved_quantity', 0)),
                'reorder_threshold': int(message.get('reorder_threshold', 0)),
                'warehouse_location': message.get('warehouse_location'),
                'shelf_location': message.get('shelf_location'),
                'status': message.get('status'),
                'last_updated': last_updated
            }
        except Exception as e:
            self.logger.error(f"Transform error: {e}")
            return None

    def _transform_shipment_message(self, message):
        try:
            # Convert microseconds to seconds properly
            created_at = datetime.fromtimestamp(message['created_at'] / 1_000_000) if message.get('created_at') else datetime.now()
            updated_at = datetime.fromtimestamp(message['updated_at'] / 1_000_000) if message.get('updated_at') else None

            return {
                'id': int(message['id']),
                'order_id': int(message['order_id']) if message.get('order_id') is not None else None,
                'shipping_address': str(message['shipping_address']),
                'billing_address': str(message['billing_address']),
                'status': str(message['status']),
                'created_at': created_at,
                'updated_at': updated_at
            }
        except Exception as e:
            self.logger.error(f"Transform error: {str(e)}", exc_info=True)
            return None

    def _transform_cart_items_message(self, message: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Cart items message transformation"""
        try:
            # Skip deleted records
            if message.get('__deleted', 'false').lower() == 'true':
                self.logger.info(f"Skipping deleted cart item record: {message.get('id')}")
                return None

            # Convert microseconds to seconds for timestamps
            created_at = datetime.fromtimestamp(
                message['created_at'] / 1_000_000) if message.get('created_at') else datetime.now()
            updated_at = datetime.fromtimestamp(
                message['updated_at'] / 1_000_000) if message.get('updated_at') else datetime.now()

            # Handle price conversion to Decimal
            price_str = str(message.get('price', '0.00'))
            try:
                price = Decimal(price_str)
            except:
                price = Decimal('0.00')

            return {
                'id': int(message.get('id', 0)),
                'user_id': int(message.get('user_id', 0)),
                'product_id': int(message.get('product_id', 0)),
                'product_name': str(message.get('product_name', '')),
                'price': price,
                'quantity': int(message.get('quantity', 0)),
                'created_at': created_at,
                'updated_at': updated_at
            }
        except Exception as e:
            self.logger.error(f"Cart items transform error: {e}", exc_info=True)
            return None

    def _transform_orders_message(self, message: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Orders message transformation"""
        try:
            # Skip deleted records
            if message.get('__deleted', 'false').lower() == 'true':
                self.logger.info(f"Skipping deleted order record: {message.get('id')}")
                return None

            # Convert microseconds to seconds for timestamps
            created_at = datetime.fromtimestamp(
                message['created_at'] / 1_000_000) if message.get('created_at') else datetime.now()
            updated_at = datetime.fromtimestamp(
                message['updated_at'] / 1_000_000) if message.get('updated_at') else datetime.now()

            # Handle total_amount conversion to Decimal
            total_amount_str = str(message.get('total_amount', '0.00'))
            try:
                total_amount = Decimal(total_amount_str)
            except:
                total_amount = Decimal('0.00')

            return {
                'id': int(message.get('id', 0)),
                'user_id': int(message.get('user_id', 0)),
                'status': str(message.get('status', '')),
                'total_amount': total_amount,
                'shipping_address': str(message.get('shipping_address', '')),
                'billing_address': str(message.get('billing_address', '')),
                'payment_method': str(message.get('payment_method', '')),
                'created_at': created_at,
                'updated_at': updated_at
            }
        except Exception as e:
            self.logger.error(f"Orders transform error: {e}", exc_info=True)
            return None

    def _transform_order_items_message(self, message: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Order items message transformation"""
        try:
            # Skip deleted records
            if message.get('__deleted', 'false').lower() == 'true':
                self.logger.info(f"Skipping deleted order item record: {message.get('id')}")
                return None

            # Convert microseconds to seconds for timestamp
            created_at = datetime.fromtimestamp(
                message['created_at'] / 1_000_000) if message.get('created_at') else datetime.now()

            # Handle unit_price conversion to Decimal
            unit_price_str = str(message.get('unit_price', '0.00'))
            try:
                unit_price = Decimal(unit_price_str)
            except:
                unit_price = Decimal('0.00')

            return {
                'id': int(message.get('id', 0)),
                'order_id': int(message.get('order_id', 0)),
                'product_id': int(message.get('product_id', 0)),
                'product_name': str(message.get('product_name', '')),
                'quantity': int(message.get('quantity', 0)),
                'unit_price': unit_price,
                'created_at': created_at
            }
        except Exception as e:
            self.logger.error(f"Order items transform error: {e}", exc_info=True)
            return None

    def process_messages(self):
        """Process messages for this topic"""
        consumer = KafkaConsumer(
            self.topic,
            **self.kafka_config,
            value_deserializer=lambda x: json.loads(x.decode('utf-8')) if x else None
        )

        ch_client = Client(**self.clickhouse_config)
        table_name = self.determine_table_name()

        try:
            for message in consumer:
                if not self.running:
                    break
                self.logger.info(message)
                transformed = self.transform_message(message.value)
                if not transformed:
                    continue

                try:
                    if 'inventory' in self.topic:
                        query = f"""
                            INSERT INTO {table_name} 
                            (id, product_id, sku, quantity_available, reserved_quantity, 
                             reorder_threshold, warehouse_location, shelf_location, status, last_updated)
                            VALUES
                        """
                    elif 'shipment' in self.topic:
                        query = f"""
                            INSERT INTO {table_name} 
                            (id, order_id, shipping_address, billing_address, status, created_at, updated_at)
                            VALUES
                        """
                    elif 'cart_items' in self.topic:
                        query = f"""
                            INSERT INTO {table_name} 
                            (id, user_id, product_id, product_name, price, quantity, created_at, updated_at)
                            VALUES
                        """
                    elif 'order_items' in self.topic:
                        query = f"""
                            INSERT INTO {table_name} 
                            (id, order_id, product_id, product_name, quantity, unit_price, created_at)
                            VALUES
                        """
                    elif 'orders' in self.topic and 'cart_items' not in self.topic and 'order_items' not in self.topic:
                        query = f"""
                            INSERT INTO {table_name} 
                            (id, user_id, status, total_amount, shipping_address, billing_address, payment_method, created_at, updated_at)
                            VALUES
                        """

                    ch_client.execute(query, [transformed])
                    self.logger.debug(f"Inserted record to {table_name}: {transformed['id']}")

                except Exception as e:
                    self.logger.error(f"Insert error: {e}")

        finally:
            consumer.close()
            ch_client.disconnect()


class KafkaClickHouseConnector:
    def __init__(self):
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        self.logger = logging.getLogger(__name__)
        self.running = True
        self.processors = []

    def setup(self):
        """Setup configuration and processors"""
        kafka_config = {
            'bootstrap_servers': ['kafka:9092'],
            'group_id': 'clickhouse-connector',
            'auto_offset_reset': 'latest',
            'enable_auto_commit': True,
            'auto_commit_interval_ms': 1000,
            'session_timeout_ms': 30000,
            'max_poll_records': 500
        }

        clickhouse_config = {
            'host': 'clickhouse',
            'port': 9000,
            'database': 'analytics',
            'user': 'admin',
            'password': 'changeme',
            'send_receive_timeout': 60,
            'sync_request_timeout': 60
        }

        topics = [
            'inventory.inventory.inventory',
            'shipment.shipment.shipment',
            'orders.ordering.cart_items',
            'orders.ordering.orders',
            'orders.ordering.order_items'
        ]

        for topic in topics:
            processor = TopicProcessor(topic, kafka_config, clickhouse_config)
            self.processors.append(processor)

    def run(self):
        """Run all processors in separate threads"""
        with ThreadPoolExecutor(max_workers=len(self.processors)) as executor:
            futures = [executor.submit(p.process_messages) for p in self.processors]
            try:
                while self.running:
                    time.sleep(1)
            except KeyboardInterrupt:
                self.shutdown()
            except Exception as e:
                self.logger.error(f"Error: {e}")
                self.shutdown()

    def shutdown(self):
        """Graceful shutdown"""
        self.running = False
        for processor in self.processors:
            processor.running = False
        self.logger.info("Shutting down all processors")


def main():
    connector = KafkaClickHouseConnector()
    connector.setup()

    # Handle signals
    def signal_handler(signum, frame):
        connector.logger.info(f"Received signal {signum}, shutting down...")
        connector.shutdown()

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    connector.run()


if __name__ == "__main__":
    main()