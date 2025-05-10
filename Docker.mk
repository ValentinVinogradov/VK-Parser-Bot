# Makefile for Docker commands
COMPOSE_CMD = docker compose $(COMPOSE_FILES)

rebuild:
	$(COMPOSE_CMD) build --no-cache
	$(COMPOSE_CMD) up -d

rebuild-c:
	$(COMPOSE_CMD) up --build -d


rebuild-o:
	$(COMPOSE_CMD) build $(S) --no-cache
	$(COMPOSE_CMD) up -d

rebuild-o-c:
	$(COMPOSE_CMD) build $(S) 
	$(COMPOSE_CMD) up -d

up:
	$(COMPOSE_CMD) up -d

oup:
	$(COMPOSE_CMD) up -d $(S)

stop:
	$(COMPOSE_CMD) stop

ostop:
	$(COMPOSE_CMD) stop $(S)

restart:
	$(COMPOSE_CMD) stop 
	$(COMPOSE_CMD) up -d 

down:
	@echo "Removing Docker containers..."
	$(COMPOSE_CMD) down
	@echo "Docker containers removed."

odown:
	@echo "Removing container..."
	$(COMPOSE_CMD) down $(S)
	@echo "Docker container removed."

exec:
	$(COMPOSE_CMD) exec -it $(S) bash

logs:
	$(COMPOSE_CMD) logs -f $(S)

status:
	$(COMPOSE_CMD) ps

reset:
	$(COMPOSE_CMD) down -v 
	$(COMPOSE_CMD) up -d

clean:
	@echo "Cleaning up containers, volumes, and dangling images..."
	$(COMPOSE_CMD) down --volumes --remove-orphans
	docker volume prune -f
	docker images -f "dangling=true" -q | xargs -r docker rmi
	@echo "Containers, volumes, and dangling images cleaned."

clean-hard: clean
	@echo "Removing images related to ${S}..."
	docker images | grep ${S} | awk '{ print $$3 }' | xargs -r docker rmi
	@echo "All ${S}-related images removed."


.PHONY: build up down logs restart clean reset rebuild exec status clean-hard orebuild stop ostop oup odown