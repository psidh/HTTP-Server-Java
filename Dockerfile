FROM node:20-alpine

WORKDIR /app

COPY . .

RUN javac ./src/main/java/Main.java


CMD ["java", "./src/main/java/Main"]
