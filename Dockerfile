FROM openjdk:21 as build
LABEL authors="foogaro"

WORKDIR /app
COPY ./target/*.jar ./app.jar

# List jar modules
RUN jar xf app.jar
RUN jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    --multi-release 21 \
    --recursive \
    --class-path 'BOOT-INF/lib/*' \
    app.jar > modules.txt

# Create a custom Java runtime
RUN jlink --add-modules $(cat modules.txt) --strip-debug --no-man-pages --no-header-files --compress=2 --output /app/javaruntime

FROM debian:buster-slim
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=build /app/javaruntime $JAVA_HOME

WORKDIR /app
COPY --from=build /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-server", "-Xms1g", "-Xmx1g", "-Xss1024k", "-XX:MaxMetaspaceSize=1g","-jar"]

CMD ["/app/app.jar"]
