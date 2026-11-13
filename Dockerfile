# Node backend (zero runtime dependencies — plain `node src/server.js`).
FROM node:22-alpine
WORKDIR /app
COPY package.json ./
COPY src ./src
COPY scripts ./scripts
COPY data/seed.json ./data/seed.json
EXPOSE 3100
CMD ["node", "src/server.js"]
