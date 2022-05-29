from typing import Callable, Awaitable


class Shutdown(Exception):
    pass


async def raise_shutdown(shutdown_event: Callable[..., Awaitable[None]], loop, scope: str) -> None:
    print("awaiting shutdown: {}".format(scope))
    await shutdown_event()
    print("shutdown received: {}".format(scope))
    raise Shutdown()
    #loop.close()
