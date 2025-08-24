NEEDS_JAR_COMMANDS = rebuild rebuild-c rebuild-o rebuild-o-c reset

build-jar:
	cd spring_app/vkparser && ./gradlew bootJar

dev-%:
	@if echo "$(filter $*, $(NEEDS_JAR_COMMANDS))" | grep -q . && \
	[ "$(S)" = "" -o "$(S)" = "vkparser" ]; then \
		$(MAKE) build-jar; \
	fi
	
	$(MAKE) -f Docker.mk $* \
	DOCKERFILE=Dockerfile.dev \
	COMPOSE_FILES="-f docker-compose.yml -f docker-compose.override.yml"


prod-%:
	$(MAKE) -f Docker.mk $* DOCKERFILE=Dockerfile.prod COMPOSE_FILES="-f docker-compose.yml"
