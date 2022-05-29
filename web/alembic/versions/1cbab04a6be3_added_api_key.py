"""added api key

Revision ID: 1cbab04a6be3
Revises: b45e34aef7fc
Create Date: 2020-11-26 13:14:08.501226

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '1cbab04a6be3'
down_revision = 'b45e34aef7fc'
branch_labels = None
depends_on = None


def upgrade():
    op.add_column('groups', sa.Column('groupapikey', sa.String(length=255), nullable=True))
    op.execute("INSERT INTO recentchanges (id, timestamp, level, description) VALUES (uuid_generate_v4(), now(), 2, \'added api key for accessing grpc service\')")


def downgrade():
    op.drop_column('groups', 'groupapikey')
