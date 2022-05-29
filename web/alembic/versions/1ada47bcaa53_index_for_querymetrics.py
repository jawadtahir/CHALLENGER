"""index for querymetrics

Revision ID: 1ada47bcaa53
Revises: 1315102ddea8
Create Date: 2021-03-25 12:04:55.523062

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
from sqlalchemy import Index

revision = '1ada47bcaa53'
down_revision = '1315102ddea8'
branch_labels = None
depends_on = None


def upgrade():
    op.create_index('idx_querymetrics_benchmark_id', 'querymetrics', ['benchmark_id'])
    op.execute("INSERT INTO recentchanges (id, timestamp, level, description) VALUES (uuid_generate_v4(), now(), 2, \'CSV export querymetrics, db-index\')")

def downgrade():
    pass
