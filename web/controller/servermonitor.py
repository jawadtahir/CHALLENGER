from datetime import datetime
import asyncio
import os
import platform
import json
from signal import SIGTERM, SIGINT
from typing import Any

import aio_pika
import psutil as psutil
from aio_pika import DeliveryMode

from shared.constants import get_config
from shared.util import raise_shutdown


def millis_interval(start, end):
    """start and end are datetime instances"""
    diff = end - start
    millis = diff.days * 24 * 60 * 60 * 1000
    millis += diff.seconds * 1000
    millis += diff.microseconds / 1000
    return millis


def get_system_metrics(disk_io_before, snapshot_time):
    n = datetime.now()

    mem = psutil.virtual_memory()
    now_disk_io = psutil.disk_io_counters()
    cpupercent = psutil.cpu_percent()
    load1m, load5m, load15m = [x / psutil.cpu_count() * 100 for x in psutil.getloadavg()]

    return dict(server_name=platform.node(),
                timestamp=datetime.now().isoformat(),
                cpu_percent=cpupercent,
                load1m=load1m,
                load5m=load5m,
                load15m=load15m,
                mem_total=mem.total,
                mem_available=mem.available,
                mem_used=mem.used,
                mem_free=mem.free,
                duration_millis=millis_interval(snapshot_time, n),
                read_count=now_disk_io.read_count - disk_io_before.read_count,
                write_count=now_disk_io.write_count - disk_io_before.write_count,
                read_bytes=now_disk_io.read_bytes - disk_io_before.read_bytes,
                write_bytes=now_disk_io.write_bytes - disk_io_before.write_bytes)


async def monitor_server_parameters(loop, shutdown_event, connection_str):
    #loop.create_task(raise_shutdown(shutdown_event.wait, loop, "servermonitor"))

    connection = await aio_pika.connect_robust(connection_str, loop=loop)
    queue_name = get_config()["servermonitor_topic"]

    async with connection:
        channel = await connection.channel()
        queue = await channel.declare_queue(queue_name, durable=True)
        last_disk_io = psutil.disk_io_counters()
        snapshot_time = datetime.now()

        while not shutdown_event.is_set():
            await asyncio.sleep(10)  # every 10 seconds, a new snapshot

            metrics = get_system_metrics(last_disk_io, snapshot_time)
            metrics_json = json.dumps(metrics)
            last_disk_io = psutil.disk_io_counters()
            snapshot_time = datetime.now()

            msg = aio_pika.Message(body=metrics_json.encode(), delivery_mode=DeliveryMode.PERSISTENT)
            await channel.default_exchange.publish(msg, routing_key=queue_name, )
            print("sent message")

        print("gracefully exiting")


if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    shutdown_event = asyncio.Event()


    def signal_handler(*_: Any) -> None:
        shutdown_event.set()

    loop.add_signal_handler(SIGTERM, signal_handler)
    loop.add_signal_handler(SIGINT, signal_handler)

    connection_str = os.environ['RABBIT_CONNECTION']

    print(connection_str)

    loop.run_until_complete(monitor_server_parameters(loop, connection_str))


