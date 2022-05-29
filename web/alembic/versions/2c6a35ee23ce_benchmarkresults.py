"""benchmarkresults

Revision ID: 2c6a35ee23ce
Revises: 79c754aa927e
Create Date: 2021-02-09 13:55:50.595443

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '2c6a35ee23ce'
down_revision = '79c754aa927e'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table('benchmarkresults',
                    sa.Column('id', sa.BigInteger(), nullable=False),
                    sa.Column('duration_sec', sa.Float(), nullable=True),
                    sa.Column('q1_count', sa.BigInteger(), nullable=True),
                    sa.Column('q1_throughput', sa.Float(), nullable=True),
                    sa.Column('q1_90percentile', sa.Float(), nullable=True),
                    sa.Column('q2_count', sa.BigInteger(), nullable=True),
                    sa.Column('q2_throughput', sa.Float(), nullable=True),
                    sa.Column('q2_90percentile', sa.Float(), nullable=True),
                    sa.Column('summary', sa.Unicode(), nullable=True),
                    sa.PrimaryKeyConstraint('id'))

    op.execute(
        "INSERT INTO recentchanges (id, timestamp, level, description) VALUES (uuid_generate_v4(), now(), 2, \'Added Benchmarkresults\')")


def downgrade():
    op.drop_table('benchmarkresults')
