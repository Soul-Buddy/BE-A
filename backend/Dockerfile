FROM amazoncorretto:21-alpine
WORKDIR /app

# 빌드 결과물인 JAR 파일 복사
# 프로젝트 구조상 build/libs 폴더 안에 plain.jar와 실행 가능한 jar가 함께 생길 수 있습니다.
# 확실하게 실행 가능한 JAR만 가져오기 위해 패턴을 조금 더 구체화하는 것이 좋습니다.
COPY build/libs/*-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
