"""create vmtable

Revision ID: 1315102ddea8
Revises: 3eebbdf9221a
Create Date: 2021-02-15 13:36:28.683558

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '1315102ddea8'
down_revision = '3eebbdf9221a'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table('virtualmachines',
                    sa.Column('id', postgresql.UUID(), nullable=False),
                    sa.Column('group_id', postgresql.UUID(), nullable=False),
                    sa.Column('internaladrs', sa.Unicode(), nullable=True),
                    sa.Column('forwardingadrs', sa.Unicode(), nullable=True),
                    sa.Column('sshpubkey', sa.Unicode(), nullable=True),
                    sa.PrimaryKeyConstraint('id')
                    )

    op.execute(
        "INSERT INTO recentchanges (id, timestamp, level, description) VALUES (uuid_generate_v4(), now(), 2, \'Create Virtualmachines\')")


def downgrade():
    op.drop_table('virtualmachines')
