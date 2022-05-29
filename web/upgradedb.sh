#!/bin/bash

pip install -e .

alembic upgrade head
