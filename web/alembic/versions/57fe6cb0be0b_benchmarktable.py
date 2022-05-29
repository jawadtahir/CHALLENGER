"""benchmarktable

Revision ID: 57fe6cb0be0b
Revises: 1cbab04a6be3
Create Date: 2021-01-28 14:07:41.276128

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql



# revision identifiers, used by Alembic.
revision = '57fe6cb0be0b'
down_revision = '1cbab04a6be3'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table('benchmarks',
                    sa.Column('id', sa.BigInteger(), nullable=False),
                    sa.Column('group_id', postgresql.UUID(), nullable=False),
                    sa.Column('timestamp', sa.TIMESTAMP(), nullable=True),
                    sa.Column('benchmark_name', sa.Unicode(), nullable=True),
                    sa.Column('benchmark_type', sa.Unicode(), nullable=True),
                    sa.Column('batchsize', sa.BigInteger(), nullable=True),
                    sa.PrimaryKeyConstraint('id')
                    )
    # ### end Alembic commands ###
    op.execute("INSERT INTO recentchanges (id, timestamp, level, description) VALUES (uuid_generate_v4(), now(), 2, \'Added Benchmarks\')")


def downgrade():
    op.drop_table('benchmarks')
