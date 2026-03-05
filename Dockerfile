# 使用预装 Maven 和 JDK21 的镜像
FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /app

# 复制源代码和配置文件
COPY pom.xml .
COPY src ./src

# 使用 Maven 执行打包
RUN mvn clean package -DskipTests

# 使用更小的运行时镜像
FROM amazoncorretto:21-alpine
WORKDIR /app

# 安装 Node.js 和 npm（用于 MCP 服务）
RUN apk add --no-cache nodejs npm

# 预先全局安装 MCP 包，避免运行时下载导致超时
RUN npm install -g @amap/amap-maps-mcp-server

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/yu-ai-agent-0.0.1-SNAPSHOT.jar app.jar

# 暴露应用端口（微信云托管默认使用 80 端口）
EXPOSE 80

# 使用生产环境配置启动应用
CMD ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]