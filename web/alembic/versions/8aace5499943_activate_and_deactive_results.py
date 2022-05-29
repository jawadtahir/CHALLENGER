"""activate and deactive results

Revision ID: 8aace5499943
Revises: 1ada47bcaa53
Create Date: 2021-03-25 14:32:54.531632

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '8aace5499943'
down_revision = '1ada47bcaa53'
branch_labels = None
depends_on = None


def upgrade():
    op.add_column('benchmarks', sa.Column('is_active', sa.Boolean(), server_default=sa.schema.DefaultClause("1"), unique=False, nullable=False, default=True))
    op.execute("INSERT INTO recentchanges (id, timestamp, level, description) VALUES (uuid_generate_v4(), now(), 2, \'Add deactivate to benchmark\')")


def downgrade():
    op.drop_column('benchmarks', 'is_active')
