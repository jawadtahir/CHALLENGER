"""merge heads

Revision ID: 3eebbdf9221a
Revises: 3ce5e479c537, 2c6a35ee23ce
Create Date: 2021-02-15 12:59:39.450921

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '3eebbdf9221a'
down_revision = ('3ce5e479c537', '2c6a35ee23ce')
branch_labels = None
depends_on = None

# this is needed because we diverged and had for a short time 2 heads, see down_revision

def upgrade():
    pass


def downgrade():
    pass
