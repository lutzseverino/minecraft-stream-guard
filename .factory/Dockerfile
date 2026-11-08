FROM node:24-bookworm-slim

# package.json pins the project's pnpm; dependencies install in each worktree.
RUN npm install --global pnpm@11.28.9 \
    && npm cache clean --force

# Keep pnpm's store off the checkout mount so project checks see only source.
ENV PNPM_CONFIG_STORE_DIR=/tmp/pnpm-store
