import logging
import asyncio

#Main helper: https://fabianlee.org/2020/02/23/kvm-testing-cloud-init-locally-using-kvm-for-an-ubuntu-cloud-image/

# More script: https://github.com/fabianlee/local-kvm-cloudimage/blob/master/ubuntu-bionic/local-km-cloudimage.sh


class VirshWrapper:
    virsh_cmd = "/usr/bin/virsh"
    image = "/home/chris/data/focal-server-cloudimg-amd64-disk-kvm.img"
    pubkey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBvAsq+Kuw6wkNWwi0Y4t4VxusemJJF3HgWc7IRjdtww chris@degree.at"

    def __init__(self, hostname):
        self.hostname = hostname

    def generate_cloud_init_cfg(self):
        cfg = """
#cloud-config
hostname: {hostname}
fqdn: test1.example.com
manage_etc_hosts: true
users:
  - name: ubuntu
    sudo: ALL=(ALL) NOPASSWD:ALL
    groups: users, admin
    home: /home/ubuntu
    shell: /bin/bash
    lock_passwd: false
    ssh-authorized-keys:
      - {pubkey}
# only cert auth via ssh (console access can still login)
ssh_pwauth: false
disable_root: false
chpasswd:
  list: |
     ubuntu:linux
  expire: False
packages:
  - qemu-guest-agent
# written to /var/log/cloud-init-output.log
final_message: "DEBS2021 Evaluation system is finally up, after $UPTIME seconds""".format(hostname=self.hostname, pubkey= pubkey)

    def generate_network_config_static(self):
        cfg = """
version: 2
ethernets:
  ens3:
     dhcp4: false
     # default libvirt network
     addresses: [ 192.168.122.158/24 ]
     gateway4: 192.168.122.1
     nameservers:
       addresses: [ 192.168.122.1,8.8.8.8 ]
     search: [ example.com ]     
        """

    async def _callVirsh(self, args):
        proc = await asyncio.create_subprocess_shell(
            cmd=self.virsh_cmd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE)

        stdout, stderr = await proc.communicate()

        return proc.returncode, stdout, stderr

class QemuImgWrapper:
    cmd = "/usr/bin/qemu-img"

    async def resize_image(self, image_path, targetpath):



def list_running_vms():
    print("hello")

def start_vm():
    print("start vm")

if __name__ == "__main__":
    logging.basicConfig()

    loop = asyncio.get_event_loop()
    wrapper = VirshWrapper()
    loop.run_until_complete(wrapper.)

    logging.getLogger().setLevel(logging.DEBUG)
    #prepare_data_directory()
    # fetch the url of the last directory
    #download_every_month()