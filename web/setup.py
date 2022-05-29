import os
from setuptools import setup, find_packages

#apt install libvirt-dev libpq-dev libjpeg-dev zlib1g-dev gfortran libblas-dev liblapack-dev

thelibFolder = os.path.dirname(os.path.realpath(__file__))
requirementPath = thelibFolder + '/requirements.txt'
install_requires = []
if os.path.isfile(requirementPath):
    with open(requirementPath) as f:
        install_requires = [line for line in map(str.lstrip, f.read().splitlines()) if
                            len(line) > 0 and not line.startswith('#')]

setup(
    name="bandency",
    version='0.1',
    description="Testing Distributed Systems under Fault",
    packages=find_packages(exclude=["tests"]),
    author="Christoph Doblander",
    install_requires=install_requires,
    extras_require={
        'dev': [
            'python-language-server[all]'
        ],
        'test': [
            'pytest', 'pyflakes'
        ]
    }
)
