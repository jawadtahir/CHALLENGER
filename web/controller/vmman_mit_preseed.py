import asyncio
import shlex

PRESEED_DIR = "/home/foobar/eclipse-workspace/bandency/web/controller"

VM_CREATE_COMMAND = "sudo /usr/bin/virt-install --name=group1 --vcpus=1 --memory=1024 --location=./firmware-10.7.0-amd64-DVD-1.iso --disk size=10 --os-variant=debian10 --initrd-inject preseed.cfg"


async def create_vm(group_no: str):

    proc = await asyncio.create_subprocess_shell(VM_CREATE_COMMAND, asyncio.subprocess.PIPE, asyncio.subprocess.PIPE, asyncio.subprocess.PIPE, cwd=PRESEED_DIR)
    sout, serr = await proc.communicate()
    print(sout.decode("utf8"))


def delete_vm(group_no: str):
    pass


if __name__ == "__main__":
    asyncio.run(create_vm("a"))
    print("fin")
