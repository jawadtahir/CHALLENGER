
# install python dependencies
sudo apt-get update
sudo apt-get upgrade
sudo apt-get dist-upgrade
sudo apt-get install build-essential python-dev python-setuptools python-pip python-smbus
sudo apt-get install libncursesw5-dev libgdbm-dev libc6-dev
sudo apt-get install zlib1g-dev libsqlite3-dev tk-dev
sudo apt-get install libssl-dev openssl
sudo apt-get install libffi-dev
sudo apt-get install lbzip2 
sudo apt-get install libreadline-dev
sudo apt-get install libpq-dev libvirt-dev

# only after the dependencies are installed
pyenv install 3.8.5
pyenv virtualenv 3.8.5 bandency-3.8.5

pip install --upgrade pip
pip install -r requirements.txt
