from datetime import datetime
import asyncio
import aio_pika
from aio_pika import DeliveryMode


async def publisher(loop, connection_str):
    connection = await aio_pika.connect_robust(connection_str, loop=loop)
    queue_name = "task_queue"

    async with connection:
        channel = await connection.channel()
        queue = await channel.declare_queue(queue_name, durable=True)
        dt_string = datetime.now().strftime("%d/%m/%Y %H:%M:%S")
        msg = aio_pika.Message(body="Hello {} - {}".format(queue_name, dt_string).encode(),
                               delivery_mode=DeliveryMode.PERSISTENT)

        await channel.default_exchange.publish(msg, routing_key=queue_name, )
        print("sent message")


async def subscriber(loop, connection_str):
    connection = await aio_pika.connect_robust(connection_str, loop=loop)
    queue_name = "task_queue"

    async with connection:
        channel = await connection.channel()
        queue = await channel.declare_queue(queue_name, durable=True)

        async with queue.iterator() as queue_iter:
            async for message in queue_iter:
                async with message.process():
                    print(message.body)
                    if queue.name in message.body.decode():
                        break


async def sleep():
    await asyncio.sleep(10)
    print("slept 10")


if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    connection_str = "amqp://bandency-prod:bandency-high-5@131.159.52.72/bandency-prod"
    loop.create_task(publisher(loop, connection_str))
    loop.create_task(subscriber(loop, connection_str))
    task10 = loop.create_task(sleep())
    loop.run_until_complete(task10)
    #loop.run_until_complete(sleep())
    loop.close()
